import { useAuthStore } from './useAuthStore';

// Mock jwt-decode
jest.mock('jwt-decode', () => ({
  jwtDecode: jest.fn((token) => {
    if (token === 'invalid_token') throw new Error('Invalid token');
    return {
      userId: '123',
      email: 'test@example.com',
      name: 'Test User',
      role: 'ADMIN',
    };
  }),
}));

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null, isHydrated: false });
  });

  it('initializes with default state', () => {
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isHydrated).toBeFalsy();
  });

  it('sets token and decodes user information correctly', () => {
    useAuthStore.getState().setToken('valid_token');
    
    const state = useAuthStore.getState();
    expect(state.token).toBe('valid_token');
    expect(state.user).toEqual({
      id: '123',
      email: 'test@example.com',
      fullName: 'Test User',
      role: 'ADMIN',
    });
  });

  it('handles invalid tokens gracefully', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    useAuthStore.getState().setToken('invalid_token');
    
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
    
    expect(consoleSpy).toHaveBeenCalledWith('Invalid token');
    consoleSpy.mockRestore();
  });

  it('logs out by clearing token and user', () => {
    useAuthStore.setState({ token: 'dummy', user: { id: '1', email: 'e', fullName: 'n', role: 'ADMIN' } });
    useAuthStore.getState().logout();
    
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
  });

  it('sets hydration state', () => {
    useAuthStore.getState().setHydrated(true);
    expect(useAuthStore.getState().isHydrated).toBeTruthy();
  });
});
