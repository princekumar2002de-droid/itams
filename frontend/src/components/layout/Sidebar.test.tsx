import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { Sidebar } from './Sidebar';
import { asUser, renderWithProviders } from '../../test/utils';

vi.mock('../../auth/AuthProvider', () => ({ useAuth: vi.fn() }));

describe('<Sidebar> role-aware navigation', () => {
  it('hides directory pages from EMPLOYEE (the API would answer 403)', () => {
    asUser(['EMPLOYEE']);
    renderWithProviders(<Sidebar />);
    expect(screen.getByRole('link', { name: /assets/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /employees/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /departments/i })).not.toBeInTheDocument();
  });

  it('shows directory pages to IT_MANAGER', () => {
    asUser(['IT_MANAGER']);
    renderWithProviders(<Sidebar />);
    expect(screen.getByRole('link', { name: /employees/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /departments/i })).toBeInTheDocument();
  });
});
