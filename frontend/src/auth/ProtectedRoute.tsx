import { type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthProvider';

/**
 * Guards a route: if the user isn't authenticated, redirects to /login
 * with a `from` state so we can bounce back after login. If `roles` is
 * passed, additionally requires one of them.
 */
export function ProtectedRoute({ children, roles }: { children: ReactNode; roles?: string[] }) {
  const { status, hasRole } = useAuth();
  const location = useLocation();

  if (status === 'checking') {
    return (
      <div className="grid h-full place-items-center text-sm text-slate-500">
        Loading…
      </div>
    );
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  if (roles && !hasRole(...roles)) {
    return (
      <div className="grid h-full place-items-center text-sm text-slate-500">
        You don't have permission to view this page.
      </div>
    );
  }
  return <>{children}</>;
}
