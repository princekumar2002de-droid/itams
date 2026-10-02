import { Inbox } from 'lucide-react';
import type { ReactNode } from 'react';

export function EmptyState({ title, description, action }:
  { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 p-10 text-center text-slate-500">
      <div className="rounded-full bg-slate-100 p-3">
        <Inbox className="h-6 w-6" aria-hidden />
      </div>
      <div className="text-sm font-medium text-slate-700">{title}</div>
      {description && <div className="max-w-md text-sm">{description}</div>}
      {action}
    </div>
  );
}
