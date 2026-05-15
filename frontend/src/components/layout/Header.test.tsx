import { render, screen, act, fireEvent } from '@testing-library/react';
import { Header } from './Header';
import { useAuthStore } from '../../store/useAuthStore';
import { apiFetch } from '../../lib/api';
import { useRouter } from 'next/navigation';

// Mock dependencies
jest.mock('../../store/useAuthStore', () => ({
  useAuthStore: jest.fn(),
}));

jest.mock('../../lib/api', () => ({
  apiFetch: jest.fn(),
}));

jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

jest.mock('lucide-react', () => ({
  Bell: () => <div data-testid="icon-bell" />,
  Search: () => <div data-testid="icon-search" />,
  LogOut: () => <div data-testid="icon-logout" />,
}));

describe('Header', () => {
  const mockLogout = jest.fn();
  const mockPush = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush });
    (useAuthStore as unknown as jest.Mock).mockImplementation((selector) => {
      if (selector.toString().includes('user')) return { fullName: 'Test User' };
      if (selector.toString().includes('logout')) return mockLogout;
      return null;
    });
    (apiFetch as jest.Mock).mockResolvedValue([]);
  });

  it('renders the header correctly for logged in user', async () => {
    await act(async () => {
      render(<Header />);
    });
    
    expect(screen.getByText('Test User')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Rechercher un audit/i)).toBeInTheDocument();
  });

  it('fetches and displays unread notifications', async () => {
    (apiFetch as jest.Mock).mockResolvedValue([{}, {}, {}]); // 3 notifications
    
    await act(async () => {
      render(<Header />);
    });
    
    expect(apiFetch).toHaveBeenCalledWith('/api/notifications/unread');
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('handles logout correctly', async () => {
    await act(async () => {
      render(<Header />);
    });
    
    const logoutBtn = screen.getByText('Déconnexion');
    fireEvent.click(logoutBtn);
    
    expect(mockLogout).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith('/login');
  });
});
