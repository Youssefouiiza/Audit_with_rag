import { apiFetch } from './api';
import { useAuthStore } from '@/store/useAuthStore';

// Mock the auth store
jest.mock('@/store/useAuthStore', () => ({
  useAuthStore: {
    getState: jest.fn(() => ({
      setToken: jest.fn(),
      logout: jest.fn()
    }))
  }
}));

describe('apiFetch', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
    
    // Mock sessionStorage
    const storageMock = {
      getItem: jest.fn(() => JSON.stringify({ state: { token: 'mock-token' } })),
      setItem: jest.fn(),
      clear: jest.fn()
    };
    Object.defineProperty(window, 'sessionStorage', { value: storageMock, writable: true });
    
    // Mock window.location
    Object.defineProperty(window, 'location', {
      value: { href: 'http://localhost/' },
      writable: true,
      configurable: true
    });
  });

  it('adds authorization header when token exists', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: jest.fn().mockResolvedValue(JSON.stringify({ data: 'test' }))
    });

    const result = await apiFetch('/api/test');
    
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/test'),
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': 'Bearer mock-token'
        })
      })
    );
    expect(result).toEqual({ data: 'test' });
  });

  it('handles 204 no content correctly', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      status: 204,
      text: jest.fn()
    });

    const result = await apiFetch('/api/test');
    expect(result).toBeNull();
  });

  it('throws error on non-ok response', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: jest.fn().mockResolvedValue({ message: 'Bad request' })
    });

    await expect(apiFetch('/api/test')).rejects.toThrow('Bad request');
  });

  it('logs out and redirects on 401 when refresh fails', async () => {
    // Return 401 for main fetch, then 401 for refresh fetch
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({ ok: false, status: 401 })
      .mockResolvedValueOnce({ ok: false, status: 401 });

    const logoutMock = jest.fn();
    (useAuthStore.getState as jest.Mock).mockReturnValue({ logout: logoutMock });

    await expect(apiFetch('/api/test')).rejects.toThrow('Session expirée');
    expect(logoutMock).toHaveBeenCalled();
    expect(window.location.href).toBe('/login');
  });
});
