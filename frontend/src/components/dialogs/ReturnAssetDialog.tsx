import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { assignmentsApi } from '../../api/assignments';
import type { AssetCondition } from '../../api/types';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Select } from '../ui/Select';
import { Textarea } from '../ui/Textarea';
import { ErrorState } from '../ui/ErrorState';

const CONDITIONS: AssetCondition[] = ['NEW', 'GOOD', 'FAIR', 'POOR'];

export function ReturnAssetDialog({ open, onClose, assignmentId, assetId, assetTag, assigneeName }:
  { open: boolean; onClose: () => void; assignmentId: number; assetId: number; assetTag: string; assigneeName: string }) {

  const qc = useQueryClient();
  const [inCondition, setCondition] = useState<AssetCondition>('GOOD');
  const [sendForMaintenance, setMaintenance] = useState(false);
  const [notes, setNotes] = useState('');

  const mutation = useMutation({
    mutationFn: () => assignmentsApi.returnIt(assignmentId, {
      inCondition, sendForMaintenance, notes: notes || undefined,
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
    mutation.mutate();
  }

  return (
    <Modal
      open={open}
      onClose={() => { mutation.reset(); onClose(); }}
      title={`Return ${assetTag}`}
      description={`Currently held by ${assigneeName}.`}
      busy={mutation.isPending}
    >
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label className="block text-xs font-medium text-slate-700">Condition returned</label>
          <Select value={inCondition} onChange={e => setCondition(e.target.value as AssetCondition)} className="mt-1">
            {CONDITIONS.map(c => <option key={c} value={c}>{c}</option>)}
          </Select>
        </div>
        <label htmlFor="return-send-maintenance" className="flex items-center gap-2 text-sm text-slate-700">
          <input id="return-send-maintenance" type="checkbox" checked={sendForMaintenance} onChange={e => setMaintenance(e.target.checked)} />
          Send for maintenance (skip going back to IN_STOCK)
        </label>
        <div>
          <label className="block text-xs font-medium text-slate-700">Notes</label>
          <Textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder="Optional" className="mt-1" />
        </div>

        {mutation.isError && <ErrorState error={mutation.error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose} disabled={mutation.isPending}>Cancel</Button>
          <Button type="submit" loading={mutation.isPending}>Confirm return</Button>
        </div>
      </form>
    </Modal>
  );
}
