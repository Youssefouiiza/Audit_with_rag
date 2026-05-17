import { renderHook } from '@testing-library/react';
import { useAuth } from './useAuth';

const mockReplace = jest.fn();
jest.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
}));

const mockApiFetch = jest.fn();
jest.mock('@/lib/api', () => ({
  apiFetch: (...args: any[]) => mockApiFetch(...args),
}));

// We need a real zustand store to test state interactions
jest.mock('@/store/useAuthStore', () => {
  let storeState: any = { token: null, user: null, isHydrated: true };
  const setState = (newState: any) => { storeState = { ...storeState, ...newState }; };
  return {
    useAuthStore: (selector?: any) => {
      if (typeof selector === 'function') return selector(storeState);
      return storeState;
    },
    __setState: setState,
    __getState: () => storeState,
  };
});

const { __setState, __getState } = require('@/store/useAuthStore');

describe('useAuth', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockApiFetch.mockResolvedValue({ fullName: 'Test User' });
  });

  it('redirects to /login when no token', () => {
    __setState({ token: null, user: null, isHydrated: true });
    renderHook(() => useAuth());
    expect(mockReplace).toHaveBeenCalledWith('/login');
  });

  it('redirects to /login when no user', () => {
    __setState({ token: 'tok', user: null, isHydrated: true });
    renderHook(() => useAuth());
    expect(mockReplace).toHaveBeenCalledWith('/login');
  });

  it('does not redirect when not hydrated', () => {
    __setState({ token: null, user: null, isHydrated: false });
    renderHook(() => useAuth());
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('returns isAuthenticated true when token exists', () => {
    __setState({ token: 'tok', user: { id: '1', email: 'e', fullName: 'U', role: 'ADMIN' }, isHydrated: true });
    const { result } = renderHook(() => useAuth());
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.token).toBe('tok');
  });

  it('redirects to role dashboard when role not in requiredRoles', () => {
    __setState({ token: 'tok', user: { id: '1', email: 'e', fullName: 'U', role: 'CLIENT' }, isHydrated: true });
    renderHook(() => useAuth(['ADMIN']));
    expect(mockReplace).toHaveBeenCalledWith('/dashboard/client');
  });

  it('does not redirect when user role is in requiredRoles', () => {
    __setState({ token: 'tok', user: { id: '1', email: 'e', fullName: 'U', role: 'ADMIN' }, isHydrated: true });
    renderHook(() => useAuth(['ADMIN', 'MANAGER']));
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('fetches fullName if missing', () => {
    __setState({ token: 'tok', user: { id: '1', email: 'e', fullName: 'Utilisateur', role: 'ADMIN' }, isHydrated: true });
    renderHook(() => useAuth());
    expect(mockApiFetch).toHaveBeenCalledWith('/api/users/me');
  });

  it('does not fetch fullName if already present', () => {
    __setState({ token: 'tok', user: { id: '1', email: 'e', fullName: 'Real Name', role: 'ADMIN' }, isHydrated: true });
    renderHook(() => useAuth());
    expect(mockApiFetch).not.toHaveBeenCalled();
  });
});
