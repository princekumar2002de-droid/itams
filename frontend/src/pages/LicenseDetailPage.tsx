import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { licensesApi } from '../api/licenses';
import { usePermissions } from '../auth/permissions';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardBody, CardHeader } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton, TableSkeleton } from '../components/ui/Skeleton';
import { AssignLicenseSeatDialog } from '../components/dialogs/AssignLicenseSeatDialog';
import { fmtDate, fmtDateTime, fmtEUR } from '../lib/format';

export function LicenseDetailPage() {
  const id = Number(useParams().id);
  const qc = useQueryClient();
  const { canManage } = usePermissions();
  const [assignOpen, setAssignOpen] = useState(false);

  const license = useQuery({ queryKey: ['license', id], queryFn: () => licensesApi.get(id), enabled: Number.isFinite(id) });
  const seats = useQuery({ queryKey: ['license-assignments', id], queryFn: () => licensesApi.assignments(id), enabled: Number.isFinite(id) });

  const release = useMutation({
    mutationFn: (assignmentId: number) => licensesApi.release(assignmentId),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['license', id] });
      await qc.invalidateQueries({ queryKey: ['license-assignments', id] });
      await qc.invalidateQueries({ queryKey: ['licenses'] });
    },
  });

  if (license.isLoading) return <><PageHeader title="Licence" /><Card><div className="p-4"><Skeleton className="h-6 w-1/3" /></div></Card></>;
  if (license.isError) return <><PageHeader title="Licence" /><ErrorState error={license.error} /></>;
  if (!license.data) return null;
  const l = license.data;
  const full = l.seatsAvailable <= 0;

  return (
    <>
      <PageHeader
        title={`${l.productVendor} ${l.productName}${l.productVersion ? ' ' + l.productVersion : ''}`}
        description={`${l.licenseReference} · ${l.licenseType.replaceAll('_', ' ')}`}
        actions={
          <div className="flex items-center gap-2">
            {canManage && <Button onClick={() => setAssignOpen(true)} disabled={full} title={full ? 'No free seats' : undefined}>Assign seat</Button>}
            <Link to="/licenses" className="inline-flex items-center gap-1 text-sm text-slate-600 hover:text-slate-900"><ArrowLeft className="h-4 w-4" /> Back to licences</Link>
          </div>
        }
      />

      <Card>
        <CardBody>
          <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm sm:grid-cols-4">
            <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Seats used</dt><dd className="mt-0.5 tabular-nums">{l.seatsUsed} / {l.seatsTotal} {full && <Badge tone="red" className="ml-1">Full</Badge>}</dd></div>
            <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Purchased</dt><dd className="mt-0.5">{fmtDate(l.purchaseDate)}</dd></div>
            <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Expires</dt><dd className="mt-0.5">{fmtDate(l.expiresOn)}</dd></div>
            <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Cost</dt><dd className="mt-0.5 tabular-nums">{fmtEUR(l.cost)}</dd></div>
          </dl>
        </CardBody>
      </Card>

      <div className="mt-6">
        <Card>
          <CardHeader><div className="text-sm font-semibold text-slate-900">Seat assignments</div></CardHeader>
          {seats.isLoading ? <TableSkeleton rows={3} /> :
           seats.isError ? <div className="p-4"><ErrorState error={seats.error} /></div> :
           seats.data && seats.data.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr><th className="px-4 py-2.5">Assigned to</th><th className="px-4 py-2.5">Since</th><th className="px-4 py-2.5">Released</th><th className="px-4 py-2.5">Status</th><th className="px-4 py-2.5" /></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {seats.data.map(s => (
                    <tr key={s.id}>
                      <td className="px-4 py-2.5">
                        {s.personName ?? (s.assetId ? <Link to={`/assets/${s.assetId}`} className="font-mono text-xs text-brand-700 hover:underline">{s.assetTag}</Link> : '—')}
                        <span className="ml-2 text-xs text-slate-500">{s.personName ? 'person' : 'device'}</span>
                      </td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDateTime(s.assignedAt)}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDateTime(s.releasedAt)}</td>
                      <td className="px-4 py-2.5">{s.open ? <Badge tone="green">Active</Badge> : <Badge tone="gray">Released</Badge>}</td>
                      <td className="px-4 py-2.5 text-right">
                        {canManage && s.open && (
                          <Button size="sm" variant="secondary" loading={release.isPending && release.variables === s.id}
                            disabled={release.isPending} onClick={() => release.mutate(s.id)}>Release</Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState title="No seats assigned" description={canManage ? 'Use “Assign seat” to give a seat to a person or device.' : 'Nobody uses this licence yet.'} />}
          {release.isError && <div className="p-4"><ErrorState error={release.error} /></div>}
        </Card>
      </div>

      <AssignLicenseSeatDialog open={assignOpen} onClose={() => setAssignOpen(false)} licenseId={l.id}
        defaultTarget={l.licenseType === 'PER_DEVICE' ? 'asset' : 'person'} />
    </>
  );
}
