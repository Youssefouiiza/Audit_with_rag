import { api } from './axios';
import { useAuthStore } from '../store/useAuthStore';

jest.mock('../store/useAuthStore', () => ({
  useAuthStore: {
    getState: jest.fn(() => ({ token: null })),
  },
}));

describe('axios api instance', () => {
  it('has correct baseURL', () => {
    expect(api.defaults.baseURL).toBe('http://localhost:8080/api');
  });

  it('adds Authorization header when token exists', async () => {
    (useAuthStore.getState as jest.Mock).mockReturnValue({ token: 'test-token' });

    const config = { headers: {} as any, method: 'get', url: '/test' };
    // Access the request interceptor
    const interceptors = (api.interceptors.request as any).handlers;
    const interceptor = interceptors[0];
    const result = interceptor.fulfilled(config);

    expect(result.headers.Authorization).toBe('Bearer test-token');
  });

  it('does not add Authorization header when no token', async () => {
    (useAuthStore.getState as jest.Mock).mockReturnValue({ token: null });

    const config = { headers: {} as any, method: 'get', url: '/test' };
    const interceptors = (api.interceptors.request as any).handlers;
    const interceptor = interceptors[0];
    const result = interceptor.fulfilled(config);

    expect(result.headers.Authorization).toBeUndefined();
  });
});
