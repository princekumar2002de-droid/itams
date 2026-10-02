import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { api, tokenStore, ApiError } from '../lib/api';

export interface AuthUser {
  userId: number;
  username: string;
  personId: number;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];   // e.g. ["ROLE_ADMIN"]
}

interface AuthContextValue {
  user: AuthUser | null;
  status: 'checking' | 'authenticated' | 'anonymous';
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /** True if the user has ANY of the given roles (without ROLE_ prefix). */
  hasRole: (...codes: string[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthContextValue['status']>('checking');
  // Server data is cached per browser tab, not per user. Whenever the signed-in user
  // changes (login, logout, session lost) the cache must be emptied — otherwise the
  // next user briefly sees the previous user's cached data, e.g. another person's
  // ticket with internal notes, even though the API would answer 404.
  const queryClient = useQueryClient();

  const bootstrap = useCallback(async () => {
    if (!tokenStore.getAccess()) { setStatus('anonymous'); return; }
    try {
      const me = await api<AuthUser>('/api/v1/auth/me');
      setUser(me);
      setStatus('authenticated');
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        tokenStore.clear();
      }
      setUser(null);
      setStatus('anonymous');
    }
  }, []);

  useEffect(() => { void bootstrap(); }, [bootstrap]);

  // Listen for the auth-lost signal emitted by the API wrapper on hard 401s.
  useEffect(() => {
    const onLost = () => { queryClient.clear(); setUser(null); setStatus('anonymous'); };
    window.addEventListener('itams:auth-lost', onLost);
    return () => window.removeEventListener('itams:auth-lost', onLost);
  }, [queryClient]);

  const login = useCallback(async (username: string, password: string) => {
    const tokens = await api<{ accessToken: string; refreshToken: string }>(
      '/api/v1/auth/login',
      { method: 'POST', body: { username, password }, noAuth: true }
    );
    tokenStore.set(tokens.accessToken, tokens.refreshToken);
    queryClient.clear();
    const me = await api<AuthUser>('/api/v1/auth/me');
    setUser(me);
    setStatus('authenticated');
  }, [queryClient]);

  const logout = useCallback(async () => {
    const refresh = tokenStore.getRefresh();
    if (refresh) {
      try { await api<void>('/api/v1/auth/logout', { method: 'POST', body: { refreshToken: refresh } }); }
      catch { /* revocation is best-effort; we clear locally regardless */ }
    }
    tokenStore.clear();
    queryClient.clear();
    setUser(null);
    setStatus('anonymous');
  }, [queryClient]);

  const hasRole = useCallback((...codes: string[]) => {
    if (!user) return false;
    const wanted = new Set(codes.map(c => `ROLE_${c}`));
    return user.roles.some(r => wanted.has(r));
  }, [user]);

  const value = useMemo(() => ({ user, status, login, logout, hasRole }),
    [user, status, login, logout, hasRole]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
