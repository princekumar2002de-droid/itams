import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { assignmentsApi } from '../../api/assignments';
import { employeesApi } from '../../api/employees';
import type { AssetCondition } from '../../api/types';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Textarea } from '../ui/Textarea';
import { ErrorState } from '../ui/ErrorState';

const CONDITIONS: AssetCondition[] = ['NEW', 'GOOD', 'FAIR', 'POOR'];

export function AssignAssetDialog({ open, onClose, assetId, assetTag }:
  { open: boolean; onClose: () => void; assetId: number; assetTag: string }) {

  const qc = useQueryClient();
  const [assigneePersonId, setAssignee] = useState<number | ''>('');
  const [expectedReturnOn, setExpected] = useState('');
  const [outCondition, setCondition] = useState<AssetCondition>('GOOD');
  const [notes, setNotes] = useState('');

  // Employees dropdown — cheap page-size.
  const employees = useQuery({
    queryKey: ['employees', 'for-assign'],
    queryFn: () => employeesApi.list({ size: 100 }),
    enabled: open,
  });

  const mutation = useMutation({
    mutationFn: () => assignmentsApi.assign({
      assetId,
      assigneePersonId: Number(assigneePersonId),
      expectedReturnOn: expectedReturnOn || undefined,
      outCondition,
      notes: notes || undefined,
    }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['asset', assetId] });
      await qc.invalidateQueries({ queryKey: ['assignments'] });
      await qc.invalidateQueries({ queryKey: ['assets'] });
      await qc.invalidateQueries({ queryKey: ['dashboard'] });
      onClose();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    if (assigneePersonId === '') return;
    mutation.mutate();
  }

  return (
    <Modal
      open={open}
      onClose={() => { mutation.reset(); onClose(); }}
      title={`Assign ${assetTag}`}
      description="Allocate this asset to an employee."
      busy={mutation.isPending}
    >
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label className="block text-xs font-medium text-slate-700">Assignee</label>
          <Select
            required
            value={assigneePersonId}
            onChange={e => setAssignee(e.target.value === '' ? '' : Number(e.target.value))}
            className="mt-1"
          >
            <option value="">Choose an employee…</option>
            {employees.data?.content.map(e => (
              <option key={e.id} value={e.personId}>
                {e.firstName} {e.lastName} ({e.employeeNumber} · {e.departmentCode})
              </option>
            ))}
          </Select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-700">Out condition</label>
            <Select value={outCondition} onChange={e => setCondition(e.target.value as AssetCondition)} className="mt-1">
              {CONDITIONS.map(c => <option key={c} value={c}>{c}</option>)}
            </Select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-700">Expected return</label>
            <Input type="date" value={expectedReturnOn} onChange={e => setExpected(e.target.value)} className="mt-1" />
          </div>
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-700">Notes</label>
          <Textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder="Optional" className="mt-1" />
        </div>

        {mutation.isError && <ErrorState error={mutation.error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose} disabled={mutation.isPending}>Cancel</Button>
          <Button type="submit" loading={mutation.isPending} disabled={assigneePersonId === ''}>Assign</Button>
        </div>
      </form>
    </Modal>
  );
}
