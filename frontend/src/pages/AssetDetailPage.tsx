import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { assetsApi } from '../api/assets';
import { assignmentsApi } from '../api/assignments';
import { maintenanceApi } from '../api/maintenance';
import { useAuth } from '../auth/AuthProvider';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardBody, CardHeader } from '../components/ui/Card';
import { AssetStatusBadge } from '../components/ui/StatusBadge';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton, TableSkeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { AssignAssetDialog } from '../components/dialogs/AssignAssetDialog';
import { ReturnAssetDialog } from '../components/dialogs/ReturnAssetDialog';
import { EditAssetDialog } from '../components/dialogs/EditAssetDialog';
import { RetireAssetDialog } from '../components/dialogs/RetireAssetDialog';
import { RecordMaintenanceDialog } from '../components/dialogs/RecordMaintenanceDialog';
import { fmtDate, fmtDateTime, fmtEUR } from '../lib/format';

export function AssetDetailPage() {
  const { id: idStr } = useParams();
  const id = Number(idStr);
  const { hasRole } = useAuth();
  const canManage = hasRole('ADMIN', 'IT_MANAGER');

  const [assignOpen, setAssignOpen] = useState(false);
  const [returnOpen, setReturnOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [retireOpen, setRetireOpen] = useState(false);
  const [maintOpen, setMaintOpen] = useState(false);
  const qc = useQueryClient();

  const maintenance = useQuery({
    queryKey: ['maintenance', 'by-asset', id],
    queryFn: () => maintenanceApi.list({ assetId: id, size: 50 }),
    enabled: Number.isFinite(id),
  });

  const completeMaintenance = useMutation({
    mutationFn: () => assetsApi.completeMaintenance(id),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['asset', id] });
      await qc.invalidateQueries({ queryKey: ['assets'] });
      await qc.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  const asset = useQuery({
    queryKey: ['asset', id],
    queryFn: () => assetsApi.get(id),
    enabled: Number.isFinite(id),
  });

  const history = useQuery({
    queryKey: ['assignments', 'by-asset', id],
    queryFn: () => assignmentsApi.list({ assetId: id, size: 50 }),
    enabled: Number.isFinite(id),
  });

  if (asset.isLoading) {
    return <>
      <PageHeader title="Asset" />
      <Card><div className="space-y-3 p-4">
        <Skeleton className="h-6 w-1/3" />
        <Skeleton className="h-4 w-2/3" />
      </div></Card>
    </>;
  }
  if (asset.isError) return <><PageHeader title="Asset" /><ErrorState error={asset.error} /></>;
  if (!asset.data) return null;

  const a = asset.data;
  const openAssignment = history.data?.content.find(x => x.open);

  return (
    <>
      <PageHeader
        title={a.assetTag}
        description={`${a.modelManufacturer} ${a.modelName} · ${a.categoryCode}`}
        actions={
          <div className="flex items-center gap-2">
            {canManage && a.status === 'IN_STOCK' && (
              <Button onClick={() => setAssignOpen(true)}>Assign</Button>
            )}
            {canManage && a.status === 'ASSIGNED' && openAssignment && (
              <Button onClick={() => setReturnOpen(true)}>Return</Button>
            )}
            {canManage && a.status === 'UNDER_MAINTENANCE' && (
              <Button loading={completeMaintenance.isPending} onClick={() => completeMaintenance.mutate()}>Maintenance done</Button>
            )}
            {canManage && a.status !== 'RETIRED' && (
              <Button variant="secondary" onClick={() => setMaintOpen(true)}>Record maintenance</Button>
            )}
            {canManage && a.status !== 'RETIRED' && (
              <Button variant="secondary" onClick={() => setEditOpen(true)}>Edit</Button>
            )}
            {canManage && (a.status === 'IN_STOCK' || a.status === 'UNDER_MAINTENANCE') && (
              <Button variant="ghost" onClick={() => setRetireOpen(true)}>Retire</Button>
            )}
            <Link to="/assets" className="inline-flex items-center gap-1 text-sm text-slate-600 hover:text-slate-900">
              <ArrowLeft className="h-4 w-4" /> Back to assets
            </Link>
          </div>
        }
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader><div className="text-sm font-semibold text-slate-900">Details</div></CardHeader>
          <CardBody>
            <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
              <Field label="Status"><AssetStatusBadge status={a.status} /></Field>
              <Field label="Serial number"><span className="font-mono text-xs">{a.serialNumber}</span></Field>
              <Field label="Purchase date">{fmtDate(a.purchaseDate)}</Field>
              <Field label="Purchase price"><span className="tabular-nums">{fmtEUR(a.purchasePrice)}</span></Field>
              <Field label="Warranty ends">{fmtDate(a.warrantyEndsOn)}</Field>
              <Field label="Created">{fmtDateTime(a.createdAt)}</Field>
              {a.retiredAt && (<>
                <Field label="Retired at">{fmtDateTime(a.retiredAt)}</Field>
                <Field label="Retired reason">{a.retiredReason ?? '—'}</Field>
              </>)}
            </dl>
            {a.notes && (
              <div className="mt-4 rounded-md bg-slate-50 p-3">
                <div className="text-xs font-medium uppercase tracking-wide text-slate-500">Notes</div>
                <div className="mt-1 whitespace-pre-wrap text-sm text-slate-700">{a.notes}</div>
              </div>
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader><div className="text-sm font-semibold text-slate-900">At a glance</div></CardHeader>
          <CardBody>
            <dl className="space-y-3 text-sm">
              <Field label="Model">{a.modelManufacturer} <span className="text-slate-500">{a.modelName}</span></Field>
              <Field label="Category">{a.categoryCode}</Field>
              <Field label="Last updated">{fmtDateTime(a.updatedAt)}</Field>
              {openAssignment && (
                <Field label="Currently with">
                  <span className="font-medium">{openAssignment.assigneeName}</span>
                  <div className="text-xs text-slate-500">since {fmtDateTime(openAssignment.assignedAt)}</div>
                </Field>
              )}
            </dl>
          </CardBody>
        </Card>
      </div>

      <div className="mt-6">
        <Card>
          <CardHeader><div className="text-sm font-semibold text-slate-900">Assignment history</div></CardHeader>
          {history.isLoading ? <TableSkeleton rows={3} /> :
           history.isError ? <div className="p-4"><ErrorState error={history.error} /></div> :
           history.data && history.data.content.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5">Assignee</th>
                    <th className="px-4 py-2.5">Assigned</th>
                    <th className="px-4 py-2.5">Returned</th>
                    <th className="px-4 py-2.5">Out</th>
                    <th className="px-4 py-2.5">In</th>
                    <th className="px-4 py-2.5">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {history.data.content.map(ax => (
                    <tr key={ax.id}>
                      <td className="px-4 py-2.5">{ax.assigneeName}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDateTime(ax.assignedAt)}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDateTime(ax.actualReturnAt)}</td>
                      <td className="px-4 py-2.5"><Badge tone="slate">{ax.outCondition}</Badge></td>
                      <td className="px-4 py-2.5">{ax.inCondition ? <Badge tone="slate">{ax.inCondition}</Badge> : '—'}</td>
                      <td className="px-4 py-2.5">
                        {ax.open ? <Badge tone="green">Open</Badge> : <Badge tone="gray">Closed</Badge>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No assignments yet" description="This asset has never been assigned." />
          )}
        </Card>
      </div>

      {completeMaintenance.isError && <ErrorState className="mt-4" error={completeMaintenance.error} />}

      <div className="mt-6">
        <Card>
          <CardHeader><div className="text-sm font-semibold text-slate-900">Maintenance history</div></CardHeader>
          {maintenance.isLoading ? <TableSkeleton rows={2} /> :
           maintenance.isError ? <div className="p-4"><ErrorState error={maintenance.error} /></div> :
           maintenance.data && maintenance.data.content.length > 0 ? (
            <ul className="divide-y divide-slate-100">
              {maintenance.data.content.map(m => (
                <li key={m.id} className="flex items-start justify-between gap-4 px-4 py-3 text-sm">
                  <div>
                    <div className="text-slate-900">{m.description}</div>
                    <div className="mt-0.5 text-xs text-slate-500">
                      {fmtDate(m.performedOn)} · {m.providerName ?? `${m.performedByName ?? `user #${m.performedByUserId}`} (internal)`}
                      {m.nextScheduledOn && <> · next due {fmtDate(m.nextScheduledOn)}</>}
                    </div>
                  </div>
                  <div className="tabular-nums text-slate-700">{fmtEUR(m.cost)}</div>
                </li>
              ))}
            </ul>
          ) : <EmptyState title="No maintenance recorded" description="Repairs and services for this asset will appear here." />}
        </Card>
      </div>

      <EditAssetDialog open={editOpen} onClose={() => setEditOpen(false)} asset={a} />
      <RetireAssetDialog open={retireOpen} onClose={() => setRetireOpen(false)} assetId={a.id} assetTag={a.assetTag} />
      <RecordMaintenanceDialog open={maintOpen} onClose={() => setMaintOpen(false)} assetId={a.id} assetTag={a.assetTag} />

      <AssignAssetDialog
        open={assignOpen}
        onClose={() => setAssignOpen(false)}
        assetId={a.id}
        assetTag={a.assetTag}
      />
      {openAssignment && (
        <ReturnAssetDialog
          open={returnOpen}
          onClose={() => setReturnOpen(false)}
          assignmentId={openAssignment.id}
          assetId={a.id}
          assetTag={a.assetTag}
          assigneeName={openAssignment.assigneeName}
        />
      )}
    </>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-0.5 text-slate-900">{children}</dd>
    </div>
  );
}
