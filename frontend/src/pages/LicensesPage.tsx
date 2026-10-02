import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { usePermissions } from '../auth/permissions';
import { Button } from '../components/ui/Button';
import { CreateLicenseDialog } from '../components/dialogs/CreateLicenseDialog';
import { licensesApi } from '../api/licenses';
import { PageHeader } from '../components/ui/PageHeader';
import { Card } from '../components/ui/Card';
import { SearchInput } from '../components/ui/SearchInput';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { TableSkeleton } from '../components/ui/Skeleton';
import { Pagination } from '../components/ui/Pagination';
import { fmtDate, fmtEUR } from '../lib/format';

const SIZE = 20;

export function LicensesPage() {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const { canManage } = usePermissions();

  const list = useQuery({
    queryKey: ['licenses', { q, page }],
    queryFn: () => licensesApi.list({ q: q || undefined, page, size: SIZE }),
  });

  return (
    <>
      <PageHeader title="Software licenses" description="Every purchased entitlement and how many seats are in use."
        actions={canManage && <Button onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4" /> New licence</Button>} />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3">
          <SearchInput value={q} onChange={v => { setQ(v); setPage(0); }}
            placeholder="Search vendor, product, reference…" className="w-full sm:w-80" />
        </div>

        {list.isLoading ? <TableSkeleton /> :
         list.isError ? <div className="p-4"><ErrorState error={list.error} /></div> :
         list.data && list.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5">Product</th>
                    <th className="px-4 py-2.5">Reference</th>
                    <th className="px-4 py-2.5">Type</th>
                    <th className="px-4 py-2.5">Seats</th>
                    <th className="px-4 py-2.5">Purchased</th>
                    <th className="px-4 py-2.5">Expires</th>
                    <th className="px-4 py-2.5 text-right">Cost</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {list.data.content.map(l => {
                    const usedPct = l.seatsTotal === 0 ? 0 : Math.round((l.seatsUsed / l.seatsTotal) * 100);
                    return (
                      <tr key={l.id} className="hover:bg-slate-50">
                        <td className="px-4 py-2.5">
                          <Link to={`/licenses/${l.id}`} className="font-medium text-brand-700 hover:underline">{l.productVendor} {l.productName}</Link>
                          <div className="text-xs text-slate-500">{l.productVersion ?? '—'}</div>
                        </td>
                        <td className="px-4 py-2.5 font-mono text-xs text-slate-600">{l.licenseReference}</td>
                        <td className="px-4 py-2.5"><Badge tone="slate">{l.licenseType.replaceAll('_', ' ')}</Badge></td>
                        <td className="px-4 py-2.5">
                          <div className="tabular-nums">
                            {l.seatsUsed} / {l.seatsTotal}
                            <span className="ml-1 text-xs text-slate-500">({usedPct}%)</span>
                          </div>
                          <div className="mt-1 h-1.5 w-24 overflow-hidden rounded bg-slate-100">
                            <div className={usedPct >= 100 ? 'h-full bg-red-500' : usedPct >= 80 ? 'h-full bg-amber-500' : 'h-full bg-brand-600'}
                                 style={{ width: `${Math.min(usedPct, 100)}%` }} />
                          </div>
                        </td>
                        <td className="px-4 py-2.5 text-slate-700">{fmtDate(l.purchaseDate)}</td>
                        <td className="px-4 py-2.5 text-slate-700">{fmtDate(l.expiresOn)}</td>
                        <td className="px-4 py-2.5 text-right tabular-nums">{fmtEUR(l.cost)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <Pagination page={list.data.page} totalPages={list.data.totalPages}
              first={list.data.first} last={list.data.last}
              totalElements={list.data.totalElements} size={list.data.size}
              onChange={setPage} />
          </>
        ) : (
          <EmptyState title="No licenses yet"
            description={canManage ? 'Use “New licence” to record your first software licence.' : 'No software licences have been recorded yet.'} />
        )}
      </Card>
      <CreateLicenseDialog open={createOpen} onClose={() => setCreateOpen(false)} />
    </>
  );
}
