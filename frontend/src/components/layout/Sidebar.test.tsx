import { render, screen } from '@testing-library/react';
import { Sidebar } from './Sidebar';
import { useAuthStore } from '../../store/useAuthStore';
import { usePathname } from 'next/navigation';

jest.mock('../../store/useAuthStore', () => ({
  useAuthStore: jest.fn(),
}));

jest.mock('next/navigation', () => ({
  usePathname: jest.fn(),
}));

jest.mock('lucide-react', () => ({
  LayoutDashboard: () => <div data-testid="icon-dashboard" />,
  Users: () => <div data-testid="icon-users" />,
  FileText: () => <div data-testid="icon-filetext" />,
  Settings: () => <div data-testid="icon-settings" />,
  ShieldCheck: () => <div data-testid="icon-shield" />,
  Bell: () => <div data-testid="icon-bell" />,
  MessageSquare: () => <div data-testid="icon-message" />,
}));

describe('Sidebar', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (usePathname as jest.Mock).mockReturnValue('/dashboard');
  });

  it('renders brand name correctly', () => {
    (useAuthStore as unknown as jest.Mock).mockImplementation((selector: any) => {
      return selector({ user: { fullName: 'Test User', role: 'ADMIN' } });
    });

    render(<Sidebar />);
    expect(screen.getByText('Audit')).toBeInTheDocument();
    expect(screen.getByText('Pro')).toBeInTheDocument();
  });

  it('renders navigation links for ADMIN', () => {
    (useAuthStore as unknown as jest.Mock).mockImplementation((selector: any) => {
      return selector({ user: { fullName: 'Admin User', role: 'ADMIN' } });
    });

    render(<Sidebar />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Utilisateurs')).toBeInTheDocument();
    expect(screen.getByText('Mes Audits')).toBeInTheDocument();
    expect(screen.getByText('Messagerie')).toBeInTheDocument();
    expect(screen.getByText('Notifications')).toBeInTheDocument();
    expect(screen.getByText('Paramètres')).toBeInTheDocument();
  });

  it('hides Utilisateurs for non-ADMIN roles', () => {
    (useAuthStore as unknown as jest.Mock).mockImplementation((selector: any) => {
      return selector({ user: { fullName: 'Auditor User', role: 'AUDITOR' } });
    });

    render(<Sidebar />);
    expect(screen.queryByText('Utilisateurs')).not.toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Mes Audits')).toBeInTheDocument();
  });

  it('displays user info in footer', () => {
    (useAuthStore as unknown as jest.Mock).mockImplementation((selector: any) => {
      return selector({ user: { fullName: 'Jean Dupont', role: 'CLIENT' } });
    });

    render(<Sidebar />);
    expect(screen.getByText('Jean Dupont')).toBeInTheDocument();
    expect(screen.getByText('CLIENT ACCOUNT')).toBeInTheDocument();
    expect(screen.getByText('J')).toBeInTheDocument(); // first letter avatar
  });

  it('shows default user info when no user', () => {
    (useAuthStore as unknown as jest.Mock).mockImplementation((selector: any) => {
      return selector({ user: null });
    });

    render(<Sidebar />);
    expect(screen.getByText('Utilisateur')).toBeInTheDocument();
    expect(screen.getByText('Guest ACCOUNT')).toBeInTheDocument();
  });

  it('renders Menu Principal label', () => {
    (useAuthStore as unknown as jest.Mock).mockImplementation((selector: any) => {
      return selector({ user: { fullName: 'Test', role: 'ADMIN' } });
    });

    render(<Sidebar />);
    expect(screen.getByText('Menu Principal')).toBeInTheDocument();
  });
});
