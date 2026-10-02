import type { LucideIcon } from 'lucide-react';
import { Card } from './Card';

export function StatCard({ label, value, hint, icon: Icon }:
  { label: string; value: string | number; hint?: string; icon?: LucideIcon }) {
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</div>
          <div className="mt-1 text-2xl font-semibold tabular-nums text-slate-900">{value}</div>
          {hint && <div className="mt-1 text-xs text-slate-500">{hint}</div>}
        </div>
        {Icon && (
          <div className="rounded-md bg-brand-50 p-2 text-brand-700">
            <Icon className="h-5 w-5" aria-hidden />
          </div>
        )}
      </div>
    </Card>
  );
}
