import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, Users, HardDrive, ArrowLeftRight, KeyRound, Wrench, LifeBuoy, User,
  Boxes, Building2
} from 'lucide-react';
import { cn } from '../../lib/cn';
import { useAuth } from '../../auth/AuthProvider';

const MANAGERS = ['ADMIN', 'IT_MANAGER'];

/** `roles` hides entries whose API the user can't call (the backend would answer 403). */
const nav: { to: string; label: string; icon: typeof User; roles?: string[] }[] = [
  { to: '/dashboard',   label: 'Dashboard',   icon: LayoutDashboard },
  { to: '/employees',   label: 'Employees',   icon: Users,     roles: MANAGERS },
  { to: '/departments', label: 'Departments', icon: Building2, roles: MANAGERS },
  { to: '/assets',      label: 'Assets',      icon: HardDrive },
  { to: '/assignments', label: 'Assignments', icon: ArrowLeftRight },
  { to: '/licenses',    label: 'Licenses',    icon: KeyRound },
  { to: '/maintenance', label: 'Maintenance', icon: Wrench },
  { to: '/tickets',     label: 'Tickets',     icon: LifeBuoy },
  { to: '/profile',     label: 'Profile',     icon: User },
];

export function Sidebar() {
  const { hasRole } = useAuth();
  return (
    <aside className="hidden w-56 flex-none border-r border-slate-200 bg-white md:flex md:flex-col">
      <div className="flex h-14 items-center gap-2 border-b border-slate-200 px-4">
        <div className="grid h-7 w-7 place-items-center rounded-md bg-brand-600 text-white">
          <Boxes className="h-4 w-4" aria-hidden />
        </div>
        <span className="text-sm font-semibold text-slate-900">ITAMS</span>
      </div>
      <nav className="flex-1 overflow-y-auto p-2">
        {nav.filter(n => !n.roles || hasRole(...n.roles)).map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium',
              isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
            )}
          >
            <Icon className="h-4 w-4" aria-hidden />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-slate-200 p-4 text-xs text-slate-500">
        <div>ITAMS v1.0.0</div>
        <div className="mt-1">IT Asset Management</div>
      </div>
    </aside>
  );
}
