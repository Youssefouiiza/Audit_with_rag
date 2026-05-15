import { render, screen } from '@testing-library/react';
import AuditProHomepage from './page';

describe('AuditProHomepage', () => {
  it('renders the main heading', () => {
    render(<AuditProHomepage />);
    const heading = screen.getByText(/L'audit d'entreprise/i);
    expect(heading).toBeInTheDocument();
  });

  it('renders the explore now button', () => {
    render(<AuditProHomepage />);
    const button = screen.getByText(/Explore Now/i);
    expect(button).toBeInTheDocument();
  });
});
