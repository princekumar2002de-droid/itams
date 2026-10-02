import { LogOut } from 'lucide-react';
import { useAuth } from '../../auth/AuthProvider';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';

export function TopBar() {
  const { user, logout } = useAuth();
  if (!user) return null;

  const initials = (user.firstName[0] + (user.lastName[0] ?? '')).toUpperCase();
  const primaryRole = user.roles[0]?.replace('ROLE_', '') ?? 'USER';

  return (
    <header className="flex h-14 flex-none items-center justify-between border-b border-slate-200 bg-white px-4">
      <div className="text-sm text-slate-500">
        {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
      </div>
      <div className="flex items-center gap-3">
        <div className="hidden text-right sm:block">
          <div className="text-sm font-medium text-slate-900">{user.firstName} {user.lastName}</div>
          <div className="text-xs text-slate-500">{user.username}</div>
        </div>
        <div className="grid h-9 w-9 place-items-center rounded-full bg-brand-100 text-sm font-semibold text-brand-700">
          {initials}
        </div>
        <Badge tone="slate">{primaryRole}</Badge>
        <Button variant="ghost" size="sm" onClick={() => void logout()} aria-label="Log out">
          <LogOut className="h-4 w-4" aria-hidden /> Log out
        </Button>
      </div>
    </header>
  );
}
