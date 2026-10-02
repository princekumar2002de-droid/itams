import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { licensesApi } from '../../api/licenses';
import { employeesApi } from '../../api/employees';
import { assetsApi } from '../../api/assets';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Select } from '../ui/Select';
import { Textarea } from '../ui/Textarea';
import { opt } from '../../lib/dates';

/** A seat goes to exactly one target: a person OR a device (backend: "assignee XOR" rule). */
export function AssignLicenseSeatDialog({ open, onClose, licenseId, defaultTarget }: {
  open: boolean; onClose: () => void; licenseId: number; defaultTarget: 'person' | 'asset';
}) {
  const qc = useQueryClient();
  const [target, setTarget] = useState<'person' | 'asset'>(defaultTarget);
  const [personId, setPersonId] = useState<number | ''>('');
  const [assetId, setAssetId] = useState<number | ''>('');
  const [notes, setNotes] = useState('');

  const employees = useQuery({ queryKey: ['employees', 'for-license'], queryFn: () => employeesApi.list({ size: 200 }), enabled: open && target === 'person' });
  const assets = useQuery({ queryKey: ['assets', 'for-license'], queryFn: () => assetsApi.list({ size: 200 }), enabled: open && target === 'asset' });

  const mutation = useMutation({
    mutationFn: () => licensesApi.assign(licenseId, {
      personId: target === 'person' ? Number(personId) : undefined,
      assetId: target === 'asset' ? Number(assetId) : undefined,
      notes: opt(notes),
    }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['license', licenseId] });
      await qc.invalidateQueries({ queryKey: ['license-assignments', licenseId] });
      await qc.invalidateQueries({ queryKey: ['licenses'] });
      close();
    },
  });

  function close() { mutation.reset(); setPersonId(''); setAssetId(''); setNotes(''); onClose(); }

  return (
    <FormDialog open={open} onClose={close} title="Assign a seat" submitLabel="Assign seat"
      onSubmit={() => mutation.mutate()} busy={mutation.isPending} error={mutation.error}
      canSubmit={target === 'person' ? personId !== '' : assetId !== ''}>
      <FormField label="Assign to">
        {id => (
          <Select id={id} value={target} onChange={e => setTarget(e.target.value as 'person' | 'asset')}>
            <option value="person">A person</option>
            <option value="asset">A device</option>
          </Select>
        )}
      </FormField>
      {target === 'person' ? (
        <FormField label="Person" required>
          {id => (
            <Select id={id} required value={personId} onChange={e => setPersonId(e.target.value === '' ? '' : Number(e.target.value))}>
              <option value="">Choose an employee…</option>
              {employees.data?.content.map(e => (
                <option key={e.id} value={e.personId}>{e.firstName} {e.lastName} ({e.employeeNumber})</option>
              ))}
            </Select>
          )}
        </FormField>
      ) : (
        <FormField label="Device" required>
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
      <FormField label="Notes">
        {id => <Textarea id={id} rows={2} value={notes} onChange={e => setNotes(e.target.value)} />}
      </FormField>
    </FormDialog>
  );
}
