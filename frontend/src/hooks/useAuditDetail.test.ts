import { renderHook, act, waitFor } from '@testing-library/react';
import { useAuditDetail } from './useAuditDetail';

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

describe('useAuditDetail', () => {
  const mockUser = { id: 'user-1', role: 'AUDITOR', fullName: 'Test' };
  const mockAudit = { id: 'audit-1', title: 'Test Audit', status: 'IN_PROGRESS' };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();

    // Default mocks: loadAudit succeeds
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/audits/')) return Promise.resolve(mockAudit);
      if (path.includes('/api/documents/audit/')) return Promise.resolve([]);
      if (path.includes('/api/reports/audit/')) return Promise.reject(new Error('not found'));
      if (path.includes('/api/ai/result/')) return Promise.reject(new Error('not found'));
      return Promise.resolve({});
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('initializes with null audit', () => {
    const { result } = renderHook(() => useAuditDetail('audit-1', null));
    expect(result.current.audit).toBeNull();
  });

  it('loads audit when user is provided', async () => {
    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    
    await waitFor(() => {
      expect(result.current.audit).not.toBeNull();
    });
    expect(result.current.audit).toEqual(mockAudit);
  });

  it('sets docs from API response', async () => {
    const mockDocs = [{ id: 'd1', fileName: 'test.pdf' }];
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/audits/')) return Promise.resolve(mockAudit);
      if (path.includes('/api/documents/audit/')) return Promise.resolve(mockDocs);
      return Promise.reject(new Error('nope'));
    });

    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    await waitFor(() => {
      expect(result.current.docs).toHaveLength(1);
    });
  });

  it('detects final report document by filename', async () => {
    const docs = [
      { id: 'd1', fileName: 'rapport_final_audit.pdf' },
      { id: 'd2', fileName: 'other.pdf' },
    ];
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/audits/')) return Promise.resolve(mockAudit);
      if (path.includes('/api/documents/audit/')) return Promise.resolve(docs);
      return Promise.reject(new Error('nope'));
    });

    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    await waitFor(() => {
      expect(result.current.finalReport).toBeTruthy();
      expect(result.current.finalReport.fileName).toContain('rapport_final');
    });
  });

  it('handles loadAudit error', async () => {
    mockApiFetch.mockRejectedValue(new Error('Network error'));
    renderHook(() => useAuditDetail('audit-1', mockUser));
    await waitFor(() => {
      expect(mockToastError).toHaveBeenCalledWith('Network error');
    });
  });

  it('changeStatus calls API and reloads', async () => {
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/status/')) return Promise.resolve({});
      if (path.includes('/api/audits/')) return Promise.resolve(mockAudit);
      if (path.includes('/api/documents/')) return Promise.resolve([]);
      return Promise.reject(new Error('nope'));
    });

    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    await waitFor(() => expect(result.current.audit).not.toBeNull());

    await act(async () => {
      await result.current.changeStatus('COMPLETED');
    });
    expect(mockApiFetch).toHaveBeenCalledWith(
      '/api/audits/audit-1/status/COMPLETED',
      { method: 'PATCH' }
    );
    expect(mockToastSuccess).toHaveBeenCalledWith('Statut mis à jour');
  });

  it('changeStatus does nothing for empty status', async () => {
    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    await act(async () => {
      await result.current.changeStatus('');
    });
    expect(mockApiFetch).not.toHaveBeenCalledWith(
      expect.stringContaining('/status/'),
      expect.anything()
    );
  });

  it('assignAuditor does nothing without selectedAuditor', async () => {
    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    await act(async () => {
      await result.current.assignAuditor();
    });
    // Should not call assign endpoint
    expect(mockApiFetch).not.toHaveBeenCalledWith(
      expect.stringContaining('/assign'),
      expect.anything()
    );
  });

  it('generateReport calls the API', async () => {
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/generate/')) return Promise.resolve({});
      if (path.includes('/api/audits/')) return Promise.resolve(mockAudit);
      if (path.includes('/api/documents/')) return Promise.resolve([]);
      return Promise.reject(new Error('nope'));
    });

    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    await waitFor(() => expect(result.current.audit).not.toBeNull());

    await act(async () => {
      await result.current.generateReport();
    });
    expect(mockApiFetch).toHaveBeenCalledWith(
      '/api/reports/generate/audit-1',
      { method: 'POST' }
    );
  });

  it('setFile updates file state', async () => {
    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    const mockFile = new File(['data'], 'test.pdf', { type: 'application/pdf' });
    act(() => {
      result.current.setFile(mockFile);
    });
    expect(result.current.file).toBe(mockFile);
  });

  it('setReviewComment updates reviewComment state', async () => {
    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    act(() => {
      result.current.setReviewComment('Looks good');
    });
    expect(result.current.reviewComment).toBe('Looks good');
  });

  it('reviewReport calls API with decision and comment', async () => {
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/review/')) return Promise.resolve({});
      if (path.includes('/api/audits/')) return Promise.resolve(mockAudit);
      if (path.includes('/api/documents/')) return Promise.resolve([]);
      return Promise.reject(new Error('nope'));
    });

    const { result } = renderHook(() => useAuditDetail('audit-1', mockUser));
    await waitFor(() => expect(result.current.audit).not.toBeNull());

    await act(async () => {
      await result.current.reviewReport('APPROVE');
    });
    expect(mockApiFetch).toHaveBeenCalledWith('/api/reports/review/audit-1', {
      method: 'PATCH',
      body: JSON.stringify({ decision: 'APPROVE', comment: '' }),
    });
  });

  it('fetches auditors for MANAGER role', async () => {
    const managerUser = { id: 'u-mgr', role: 'MANAGER', fullName: 'Mgr' };
    mockApiFetch.mockImplementation((path: string) => {
      if (path.includes('/api/audits/audit-1') && !path.includes('status'))
        return Promise.resolve(mockAudit);
      if (path.includes('/api/documents/')) return Promise.resolve([]);
      if (path.includes('/api/users?role=AUDITOR'))
        return Promise.resolve({ content: [{ id: 'a1', fullName: 'Aud1' }] });
      return Promise.reject(new Error('nope'));
    });

    const { result } = renderHook(() => useAuditDetail('audit-1', managerUser));
    await waitFor(() => {
      expect(result.current.auditors).toHaveLength(1);
    });
  });
});
