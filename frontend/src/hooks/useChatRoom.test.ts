import { renderHook, act, waitFor } from '@testing-library/react';
import { useChatRoom } from './useChatRoom';

const mockApiFetch = jest.fn();
jest.mock('@/lib/api', () => ({
  apiFetch: (...args: any[]) => mockApiFetch(...args),
}));

const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
jest.mock('react-hot-toast', () => ({
  __esModule: true,
  default: {
    success: (...args: any[]) => mockToastSuccess(...args),
    error: (...args: any[]) => mockToastError(...args),
  },
}));

describe('useChatRoom', () => {
  const mockUser = { id: 'user-1', fullName: 'Test User', role: 'AUDITOR' };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    // Mock sessionStorage
    Object.defineProperty(window, 'sessionStorage', {
      value: {
        getItem: jest.fn(() => JSON.stringify({ state: { token: 'mock-token' } })),
        setItem: jest.fn(),
        clear: jest.fn(),
      },
      writable: true,
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('loads my audits when no auditId provided', async () => {
    mockApiFetch.mockResolvedValue({ content: [{ id: 'a1', title: 'Audit 1' }] });
    const { result } = renderHook(() => useChatRoom(null, mockUser));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.myAudits).toHaveLength(1);
    expect(mockApiFetch).toHaveBeenCalledWith('/api/audits/mine?size=100');
  });

  it('loads room when auditId is provided', async () => {
    const mockRoom = { id: 'room-1', auditId: 'audit-1' };
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/chat/room/audit/')) return Promise.resolve(mockRoom);
      if (path.includes('/messages')) return Promise.resolve({ content: [] });
      return Promise.resolve({});
    });

    const { result } = renderHook(() => useChatRoom('audit-1', mockUser));
    await waitFor(() => {
      expect(result.current.room).toEqual(mockRoom);
      expect(result.current.loading).toBe(false);
    });
  });

  it('shows error when auditId is null and loadRoom is called', async () => {
    mockApiFetch.mockResolvedValue({ content: [] });
    const { result } = renderHook(() => useChatRoom(null, mockUser));
    
    await waitFor(() => expect(result.current.loading).toBe(false));
    
    // Manually trigger loadRoom without auditId
    await act(async () => {
      await result.current.loadRoom();
    });
    expect(mockToastError).toHaveBeenCalledWith('Aucun audit spécifié pour ce chat');
  });

  it('loads and sorts messages', async () => {
    const mockRoom = { id: 'room-1' };
    const mockMessages = [
      { id: 'm2', content: 'Second', createdAt: '2024-01-02T00:00:00Z' },
      { id: 'm1', content: 'First', createdAt: '2024-01-01T00:00:00Z' },
    ];
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/chat/room/audit/')) return Promise.resolve(mockRoom);
      if (path.includes('/messages')) return Promise.resolve({ content: mockMessages });
      return Promise.resolve({});
    });

    const { result } = renderHook(() => useChatRoom('audit-1', mockUser));
    await waitFor(() => {
      expect(result.current.messages).toHaveLength(2);
      // Messages should be sorted chronologically
      expect(result.current.messages[0].id).toBe('m1');
      expect(result.current.messages[1].id).toBe('m2');
    });
  });

  it('setInputText updates input state', () => {
    mockApiFetch.mockResolvedValue({ content: [] });
    const { result } = renderHook(() => useChatRoom(null, mockUser));
    act(() => {
      result.current.setInputText('Hello world');
    });
    expect(result.current.inputText).toBe('Hello world');
  });

  it('handleSend does nothing on empty input', async () => {
    mockApiFetch.mockResolvedValue({ content: [] });
    const { result } = renderHook(() => useChatRoom(null, mockUser));
    
    const mockEvent = { preventDefault: jest.fn() } as any;
    await act(async () => {
      await result.current.handleSend(mockEvent);
    });
    expect(mockEvent.preventDefault).toHaveBeenCalled();
  });

  it('handleSend adds optimistic message then replaces on success', async () => {
    const mockRoom = { id: 'room-1' };
    const savedMsg = { id: 'saved-1', content: 'Hi', senderId: 'user-1', senderName: 'Test User', createdAt: '2024-01-01T00:00:00Z', messageType: 'TEXT' };
    
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/chat/room/audit/')) return Promise.resolve(mockRoom);
      if (path.includes('/messages') && !path.includes('POST')) return Promise.resolve({ content: [] });
      return Promise.resolve({});
    });

    // Mock global fetch for the send
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue(savedMsg),
    });

    const { result } = renderHook(() => useChatRoom('audit-1', mockUser));
    await waitFor(() => expect(result.current.room).toEqual(mockRoom));

    act(() => {
      result.current.setInputText('Hi');
    });

    const mockEvent = { preventDefault: jest.fn() } as any;
    await act(async () => {
      await result.current.handleSend(mockEvent);
    });
    expect(result.current.inputText).toBe('');
    expect(result.current.sending).toBe(false);
  });

  it('handleSend removes optimistic message on failure', async () => {
    const mockRoom = { id: 'room-1' };
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/chat/room/audit/')) return Promise.resolve(mockRoom);
      if (path.includes('/messages')) return Promise.resolve({ content: [] });
      return Promise.resolve({});
    });

    global.fetch = jest.fn().mockResolvedValue({ ok: false });

    const { result } = renderHook(() => useChatRoom('audit-1', mockUser));
    await waitFor(() => expect(result.current.room).toEqual(mockRoom));

    act(() => result.current.setInputText('Fail msg'));
    const mockEvent = { preventDefault: jest.fn() } as any;
    await act(async () => {
      await result.current.handleSend(mockEvent);
    });
    expect(mockToastError).toHaveBeenCalledWith('Envoi échoué');
  });
});
