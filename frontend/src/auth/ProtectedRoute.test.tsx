import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { useAuth } from './AuthProvider';

vi.mock('./AuthProvider', () => ({ useAuth: vi.fn() }));
const mockedUseAuth = vi.mocked(useAuth);

function authState(status: 'checking' | 'authenticated' | 'anonymous', roles: string[] = []) {
  mockedUseAuth.mockReturnValue({
    status,
    user: null,
    login: vi.fn(),
    logout: vi.fn(),
    hasRole: (...codes: string[]) => codes.some(c => roles.includes(c)),
  });
}

function renderAt(path: string, roles?: string[]) {
  render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/login" element={<p>login page</p>} />
        <Route path="/licenses" element={<ProtectedRoute roles={roles}><p>secret content</p></ProtectedRoute>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('<ProtectedRoute>', () => {
  it('shows a loading state while the session is being checked', () => {
    authState('checking');
    renderAt('/licenses');
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
    expect(screen.queryByText('secret content')).not.toBeInTheDocument();
  });

  it('redirects anonymous users to /login', () => {
    authState('anonymous');
    renderAt('/licenses');
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('blocks an authenticated user without the required role', () => {
    authState('authenticated', ['EMPLOYEE']);
    renderAt('/licenses', ['ADMIN', 'IT_MANAGER']);
    expect(screen.getByText(/don't have permission/i)).toBeInTheDocument();
    expect(screen.queryByText('secret content')).not.toBeInTheDocument();
  });

  it('renders the page for a user with one of the required roles', () => {
    authState('authenticated', ['IT_MANAGER']);
    renderAt('/licenses', ['ADMIN', 'IT_MANAGER']);
    expect(screen.getByText('secret content')).toBeInTheDocument();
  });
});
