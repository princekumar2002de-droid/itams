import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';
import { useAuth } from '../auth/AuthProvider';

/** Requires `vi.mock('<path>/auth/AuthProvider', () => ({ useAuth: vi.fn() }))` in the test file. */
export function asUser(roles: string[], ids: { userId?: number; personId?: number } = {}) {
  vi.mocked(useAuth).mockReturnValue({
    status: 'authenticated',
    user: { userId: ids.userId ?? 7, username: 'u', personId: ids.personId ?? 70, firstName: 'U', lastName: 'Ser',
            email: 'u@x', roles: roles.map(r => `ROLE_${r}`) },
    login: vi.fn(), logout: vi.fn(),
    hasRole: (...codes: string[]) => codes.some(c => roles.includes(c)),
  });
}

export function renderWithProviders(ui: ReactElement, { path = '/', route = '/' } = {}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[route]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes><Route path={path} element={ui} /><Route path="*" element={<div>navigated</div>} /></Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
