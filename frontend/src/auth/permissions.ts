import { useAuth } from './AuthProvider';

/**
 * UI-side mirror of the backend's @PreAuthorize rules, used only to decide which
 * buttons and menu entries to SHOW. The backend still enforces every rule — hiding
 * a button is about usability, not security.
 *
 *   canManage → ADMIN or IT_MANAGER (assets, assignments, licenses, maintenance, ticket triage)
 *   isAdmin   → ADMIN only (people, employees, departments)
 */
export function usePermissions() {
  const { hasRole, user } = useAuth();
  return {
    canManage: hasRole('ADMIN', 'IT_MANAGER'),
    isAdmin: hasRole('ADMIN'),
    userId: user?.userId ?? null,
    personId: user?.personId ?? null,
  };
}
