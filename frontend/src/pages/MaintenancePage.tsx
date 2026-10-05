import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { maintenanceApi } from '../api/maintenance';
import { PageHeader } from '../components/ui/PageHeader';
import { Card } from '../components/ui/Card';
import { SearchInput } from '../components/ui/SearchInput';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { TableSkeleton } from '../components/ui/Skeleton';
import { Pagination } from '../components/ui/Pagination';
import { Badge } from '../components/ui/Badge';
import { fmtDate, fmtEUR } from '../lib/format';
import { Plus } from 'lucide-react';
import { usePermissions } from '../auth/permissions';
import { Button } from '../components/ui/Button';
import { RecordMaintenanceDialog } from '../components/dialogs/RecordMaintenanceDialog';
import { ConfirmDialog } from '../components/form/ConfirmDialog';
import type { MaintenanceRecord } from '../api/types';

const SIZE = 20;

export function MaintenancePage() {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [deleting, setDeleting] = useState<MaintenanceRecord | null>(null);
  const { canManage } = usePermissions();
  const qc = useQueryClient();
  const remove = useMutation({
    mutationFn: (id: number) => maintenanceApi.remove(id),
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: ['maintenance'] }); setDeleting(null); },
  });

  const list = useQuery({
    queryKey: ['maintenance', { q, page }],
    queryFn: () => maintenanceApi.list({ q: q || undefined, page, size: SIZE }),
  });

  return (
    <>
      <PageHeader title="Maintenance" description="Repair and service history for every asset."
        actions={canManage && <Button onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4" /> Record maintenance</Button>} />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3">
          <SearchInput value={q} onChange={v => { setQ(v); setPage(0); }}
            placeholder="Search asset tag, provider, description…" className="w-full sm:w-80" />
        </div>

        {list.isLoading ? <TableSkeleton /> :
         list.isError ? <div className="p-4"><ErrorState error={list.error} /></div> :
         list.data && list.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5">Asset</th>
                    <th className="px-4 py-2.5">Performed on</th>
                    <th className="px-4 py-2.5">By</th>
                    <th className="px-4 py-2.5">Description</th>
                    <th className="px-4 py-2.5">Next scheduled</th>
                    <th className="px-4 py-2.5 text-right">Cost</th>
                    {canManage && <th className="px-4 py-2.5" />}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {list.data.content.map(m => (
                    <tr key={m.id} className="hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-mono text-xs">
                        <Link to={`/assets/${m.assetId}`} className="text-brand-700 hover:underline">{m.assetTag}</Link>
                      </td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDate(m.performedOn)}</td>
                      <td className="px-4 py-2.5">
                        {m.providerName
                          ? <span className="text-slate-700">{m.providerName} <Badge tone="slate" className="ml-1">External</Badge></span>
                          : <span className="text-slate-700">{m.performedByName ?? `User #${m.performedByUserId}`} <Badge tone="blue" className="ml-1">Internal</Badge></span>
                        }
                      </td>
                      <td className="px-4 py-2.5 text-slate-700 max-w-md truncate" title={m.description}>{m.description}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDate(m.nextScheduledOn)}</td>
                      <td className="px-4 py-2.5 text-right tabular-nums">{fmtEUR(m.cost)}</td>
                      {canManage && (
                        <td className="px-4 py-2.5 text-right">
                          <Button size="sm" variant="ghost" onClick={() => { remove.reset(); setDeleting(m); }}>Delete</Button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={list.data.page} totalPages={list.data.totalPages}
              first={list.data.first} last={list.data.last}
              totalElements={list.data.totalElements} size={list.data.size}
              onChange={setPage} />
          </>
        ) : (
          <EmptyState title="No maintenance records yet"
            description={canManage ? 'Use “Record maintenance” to log a repair or service.' : 'No repairs or services have been recorded yet.'} />
        )}
      </Card>
      <RecordMaintenanceDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <ConfirmDialog open={deleting != null} onClose={() => setDeleting(null)}
        title="Delete maintenance record?"
        message={deleting ? `${deleting.assetTag} · ${fmtDate(deleting.performedOn)} — ${deleting.description}` : ''}
        onConfirm={() => deleting && remove.mutate(deleting.id)} busy={remove.isPending} error={remove.error} />
    </>
  );
}
