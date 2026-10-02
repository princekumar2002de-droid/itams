import { useQuery } from '@tanstack/react-query';
import { HardDrive, Users, ArrowLeftRight, Wrench, LifeBuoy, KeyRound, Archive, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { statsApi } from '../api/stats';
import { PageHeader } from '../components/ui/PageHeader';
import { StatCard } from '../components/ui/StatCard';
import { Card, CardBody, CardHeader } from '../components/ui/Card';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton } from '../components/ui/Skeleton';
import type { CountByLabel } from '../api/types';

export function DashboardPage() {
  const stats = useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: () => statsApi.dashboard(),
  });

  if (stats.isError) {
    return <>
      <PageHeader title="Dashboard" />
      <ErrorState error={stats.error} />
    </>;
  }

  const d = stats.data;
  const loading = stats.isLoading || !d;

  return (
    <>
      <PageHeader title="Dashboard" description="Live view of the ITAMS estate — data straight from the database." />

      {/* KPI row */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Total assets"        value={loading ? '…' : d.totalAssets}            icon={HardDrive} />
        <StatCard label="Available"           value={loading ? '…' : d.availableAssets}
                  hint="Ready to assign" icon={CheckCircle2} />
        <StatCard label="Assigned now"        value={loading ? '…' : d.assignedAssets}
                  hint="With employees"  icon={ArrowLeftRight} />
        <StatCard label="Under maintenance"   value={loading ? '…' : d.assetsUnderMaintenance} icon={Wrench} />
      </div>

      <div className="mt-4 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Employees"           value={loading ? '…' : d.totalEmployees}         icon={Users} />
        <StatCard label="Open tickets"        value={loading ? '…' : d.openTickets}            icon={LifeBuoy} />
        <StatCard label="Licenses expiring"   value={loading ? '…' : d.licensesExpiringSoon}
                  hint="Within 60 days"   icon={KeyRound} />
        <StatCard label="Retired assets"      value={loading ? '…' : d.retiredAssets}          icon={Archive} />
      </div>

      {/* Two-column breakdowns */}
      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <BarBreakdownCard title="Assets by category" data={d?.assetsByCategory} loading={loading} emptyHint="Register some assets to see the breakdown." />
        <BarBreakdownCard title="Assets by department" data={d?.assetsByDepartment} loading={loading} emptyHint="Assign assets to employees to see distribution across departments." />
        <BarBreakdownCard title="Assets by status" data={d?.assetsByStatus} loading={loading} formatter={statusLabel} />
        <BarBreakdownCard title="Open tickets by priority" data={d?.ticketsByPriority} loading={loading} emptyHint="No open tickets right now." tone="warn" />
      </div>

      <div className="mt-6 text-xs text-slate-500">
        Data as of {new Date().toLocaleString('en-GB')}.
        <Link to="/assets" className="ml-2 text-brand-700 hover:underline">Manage assets →</Link>
        <Link to="/tickets" className="ml-4 text-brand-700 hover:underline">Manage tickets →</Link>
      </div>
    </>
  );
}

// ── breakdown card ──────────────────────────────────────────────────────────

function BarBreakdownCard({ title, data, loading, emptyHint, formatter, tone = 'brand' }: {
  title: string;
  data?: CountByLabel[];
  loading: boolean;
  emptyHint?: string;
  formatter?: (raw: string) => string;
  tone?: 'brand' | 'warn';
}) {
  return (
    <Card>
      <CardHeader><div className="text-sm font-semibold text-slate-900">{title}</div></CardHeader>
      <CardBody>
        {loading ? (
          <div className="space-y-2">{[0,1,2,3].map(i => <Skeleton key={i} className="h-6 w-full" />)}</div>
        ) : !data || data.length === 0 || data.every(d => d.count === 0) ? (
          <div className="py-4 text-center text-sm text-slate-500">{emptyHint ?? 'No data.'}</div>
        ) : (
          <ul className="space-y-2.5">
            {(() => {
              const max = Math.max(...data.map(d => d.count), 1);
              return data.map(row => (
                <li key={row.label} className="text-sm">
                  <div className="flex items-center justify-between">
                    <span className="truncate text-slate-700">{formatter ? formatter(row.label) : row.label}</span>
                    <span className="ml-2 tabular-nums font-medium text-slate-900">{row.count}</span>
                  </div>
                  <div className="mt-1 h-2 overflow-hidden rounded bg-slate-100">
                    <div
                      className={tone === 'warn' ? 'h-full bg-amber-500' : 'h-full bg-brand-600'}
                      style={{ width: `${Math.round((row.count / max) * 100)}%` }}
                    />
                  </div>
                </li>
              ));
            })()}
          </ul>
        )}
      </CardBody>
    </Card>
  );
}

function statusLabel(raw: string): string {
  return raw.replaceAll('_', ' ').replace(/\b\w/g, c => c.toUpperCase()).replace(/^In Stock/i, 'In Stock');
}
