import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { assetsApi } from '../../api/assets';
import { maintenanceApi } from '../../api/maintenance';
import { usePermissions } from '../../auth/permissions';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Textarea } from '../ui/Textarea';
import { opt, todayIso } from '../../lib/dates';

/**
 * Records a maintenance event. Business rule (MaintenanceService): exactly one performer —
 * done internally (recorded as the current user) or by an external provider.
 */
export function RecordMaintenanceDialog({ open, onClose, assetId: fixedAssetId, assetTag }: {
  open: boolean; onClose: () => void; assetId?: number; assetTag?: string;
}) {
  const qc = useQueryClient();
  const { userId } = usePermissions();
  const [assetId, setAssetId] = useState<number | ''>(fixedAssetId ?? '');
  const [performedOn, setPerformedOn] = useState(todayIso());
  const [performer, setPerformer] = useState<'internal' | 'external'>('internal');
  const [providerName, setProvider] = useState('');
  const [description, setDescription] = useState('');
  const [cost, setCost] = useState('');
  const [nextScheduledOn, setNext] = useState('');

  const assets = useQuery({
    queryKey: ['assets', 'for-maintenance'],
    queryFn: () => assetsApi.list({ size: 200 }),
    enabled: open && fixedAssetId === undefined,
  });

  const mutation = useMutation({
    mutationFn: () => maintenanceApi.create({
      assetId: Number(fixedAssetId ?? assetId),
      performedOn,
      performedByUserId: performer === 'internal' ? userId ?? undefined : undefined,
      providerName: performer === 'external' ? opt(providerName) : undefined,
      description: description.trim(),
      cost: cost === '' ? undefined : Number(cost),
      nextScheduledOn: opt(nextScheduledOn),
    }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['maintenance'] });
      close();
    },
  });

  function close() {
    mutation.reset();
    if (fixedAssetId === undefined) setAssetId('');
    setPerformedOn(todayIso()); setPerformer('internal'); setProvider(''); setDescription(''); setCost(''); setNext('');
    onClose();
  }

  const canSubmit = (fixedAssetId !== undefined || assetId !== '') && performedOn !== '' && description.trim() !== ''
    && (performer === 'internal' ? userId != null : providerName.trim() !== '');

  return (
    <FormDialog open={open} onClose={close} title={assetTag ? `Record maintenance — ${assetTag}` : 'Record maintenance'}
      submitLabel="Save record" onSubmit={() => mutation.mutate()} busy={mutation.isPending}
      error={mutation.error} canSubmit={canSubmit}>
      {fixedAssetId === undefined && (
        <FormField label="Asset" required>
          {id => (
            <Select id={id} required value={assetId} onChange={e => setAssetId(e.target.value === '' ? '' : Number(e.target.value))}>
              <option value="">Choose an asset…</option>
              {assets.data?.content.filter(a => a.status !== 'RETIRED').map(a => (
                <option key={a.id} value={a.id}>{a.assetTag} — {a.modelManufacturer} {a.modelName}</option>
              ))}
            </Select>
          )}
        </FormField>
      )}
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Performed on" required>
          {id => <Input id={id} type="date" required max={todayIso()} value={performedOn} onChange={e => setPerformedOn(e.target.value)} />}
        </FormField>
        <FormField label="Performed by" required>
          {id => (
            <Select id={id} value={performer} onChange={e => setPerformer(e.target.value as 'internal' | 'external')}>
              <option value="internal">Me (internal IT)</option>
              <option value="external">External provider</option>
            </Select>
          )}
        </FormField>
      </div>
      {performer === 'external' && (
        <FormField label="Provider name" required>
          {id => <Input id={id} required maxLength={160} value={providerName} onChange={e => setProvider(e.target.value)} placeholder="e.g. Dell ProSupport" />}
        </FormField>
      )}
      <FormField label="Description" required>
        {id => <Textarea id={id} required rows={3} value={description} onChange={e => setDescription(e.target.value)} placeholder="What was done?" />}
      </FormField>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Cost (EUR)">
          {id => <Input id={id} type="number" min="0" step="0.01" value={cost} onChange={e => setCost(e.target.value)} />}
        </FormField>
        <FormField label="Next service due">
          {id => <Input id={id} type="date" value={nextScheduledOn} onChange={e => setNext(e.target.value)} />}
        </FormField>
      </div>
    </FormDialog>
  );
}
