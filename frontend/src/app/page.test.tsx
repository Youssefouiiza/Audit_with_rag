import { render, screen } from '@testing-library/react';
import AuditProHomepage from './page';

// Mock the lucide-react icons since they can cause issues in jest
jest.mock('lucide-react', () => ({
  ArrowRight: () => <div data-testid="icon-arrow-right" />,
  Database: () => <div data-testid="icon-database" />,
  Globe: () => <div data-testid="icon-globe" />,
  Shield: () => <div data-testid="icon-shield" />,
  User: () => <div data-testid="icon-user" />,
}));

describe('AuditProHomepage', () => {
  it('renders the navigation correctly', () => {
    render(<AuditProHomepage />);
    
    // Check brand name
    expect(screen.getByText('AuditPro')).toBeInTheDocument();
    
    // Check sign in link
    const signInLink = screen.getByText('Sign In');
    expect(signInLink).toBeInTheDocument();
    expect(signInLink.closest('a')).toHaveAttribute('href', '/login');
  });

  it('renders the hero section with main content', () => {
    render(<AuditProHomepage />);
    
    // Check main heading
    expect(screen.getByText(/L'audit d'entreprise,/i)).toBeInTheDocument();
    expect(screen.getByText(/propulsé par/i)).toBeInTheDocument();
    expect(screen.getByText(/l'intelligence artificielle/i)).toBeInTheDocument();
    
    // Check description text
    expect(screen.getByText(/Sécurisez la conformité CGNC et IFRS/i)).toBeInTheDocument();
    
    // Check call to action button
    const exploreButton = screen.getByText('Explore Now');
    expect(exploreButton).toBeInTheDocument();
    expect(exploreButton.closest('a')).toHaveAttribute('href', '/dashboard');
  });

  it('renders the feature tags', () => {
    render(<AuditProHomepage />);
    
    expect(screen.getByText('RAG Online')).toBeInTheDocument();
    expect(screen.getByText('Ar/Fr/En')).toBeInTheDocument();
    expect(screen.getByText('CGNC / IFRS')).toBeInTheDocument();
  });

  it('renders the floating person cards', () => {
    render(<AuditProHomepage />);
    
    expect(screen.getByText('Youssef A.')).toBeInTheDocument();
    expect(screen.getByText('Directeur Audit')).toBeInTheDocument();
    
    expect(screen.getByText('Fatima Z.')).toBeInTheDocument();
    expect(screen.getByText('Manager Senior')).toBeInTheDocument();
    
    expect(screen.getByText('Karim B.')).toBeInTheDocument();
    expect(screen.getByText('Auditeur Senior')).toBeInTheDocument();
  });

  it('renders the footer', () => {
    render(<AuditProHomepage />);
    
    expect(screen.getByText('Documentation')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
    expect(screen.getByText('Support')).toBeInTheDocument();
    expect(screen.getByText('Confidentialité')).toBeInTheDocument();
    expect(screen.getByText(/AuditPro SaaS\. Tous droits réservés\./i)).toBeInTheDocument();
  });
});
