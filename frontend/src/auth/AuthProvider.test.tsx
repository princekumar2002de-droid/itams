import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { AuthProvider, useAuth } from './AuthProvider';
import { tokenStore } from '../lib/api';

const fetchMock = vi.fn<typeof fetch>();
beforeEach(() => { fetchMock.mockReset(); vi.stubGlobal('fetch', fetchMock); });

let qc: QueryClient;
function withQuery(children: ReactNode) {
  qc = new QueryClient();
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

let logoutFn: (() => Promise<void>) | null = null;
function Probe() {
  const { status, user, hasRole, logout } = useAuth();
  logoutFn = logout;
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="user">{user?.username ?? '-'}</span>
      <span data-testid="is-admin">{String(hasRole('ADMIN'))}</span>
      <span data-testid="is-manager-or-admin">{String(hasRole('ADMIN', 'IT_MANAGER'))}</span>
    </div>
  );
}

const me = {
  userId: 2, username: 'maria', personId: 5, firstName: 'Maria', lastName: 'S',
  email: 'maria@example.com', roles: ['ROLE_IT_MANAGER'],
};

describe('<AuthProvider>', () => {
  it('is anonymous without a stored token and makes no network call', async () => {
    render(withQuery(<AuthProvider><Probe /></AuthProvider>));
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('restores the session from /me and evaluates roles without the ROLE_ prefix', async () => {
    tokenStore.set('access', 'refresh');
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(me), {
      status: 200, headers: { 'content-type': 'application/json' },
    }));

    render(withQuery(<AuthProvider><Probe /></AuthProvider>));

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));
    expect(screen.getByTestId('user')).toHaveTextContent('maria');
    expect(screen.getByTestId('is-admin')).toHaveTextContent('false');
    expect(screen.getByTestId('is-manager-or-admin')).toHaveTextContent('true');
  });

  it('drops to anonymous when the API signals auth-lost', async () => {
    tokenStore.set('access', 'refresh');
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(me), {
      status: 200, headers: { 'content-type': 'application/json' },
    }));
    render(withQuery(<AuthProvider><Probe /></AuthProvider>));
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    window.dispatchEvent(new CustomEvent('itams:auth-lost'));

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
  });

  it('logout empties the data cache so the next user cannot see the previous user\'s data', async () => {
    tokenStore.set('access', 'refresh');
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify(me), { status: 200, headers: { 'content-type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));   // POST /auth/logout
    render(withQuery(<AuthProvider><Probe /></AuthProvider>));
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));
    qc.setQueryData(['ticket', 1], { subject: 'admin-only', comments: [{ internal: true }] });

    await act(async () => { await logoutFn!(); });

    expect(qc.getQueryData(['ticket', 1])).toBeUndefined();
    expect(screen.getByTestId('status')).toHaveTextContent('anonymous');
  });
});
