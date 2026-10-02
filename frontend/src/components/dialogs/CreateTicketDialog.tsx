import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ticketsApi } from '../../api/tickets';
import { useAuth } from '../../auth/AuthProvider';
import type { TicketPriority } from '../../api/types';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Textarea } from '../ui/Textarea';
import { ErrorState } from '../ui/ErrorState';

const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function CreateTicketDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user } = useAuth();
  const qc = useQueryClient();

  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [relatedAssetId, setRelated] = useState('');

  const mutation = useMutation({
    mutationFn: () => ticketsApi.create({
      subject: subject.trim(),
      description: description.trim(),
      priority,
      reporterPersonId: user!.personId,
      relatedAssetId: relatedAssetId.trim() ? Number(relatedAssetId) : undefined,
    }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['tickets'] });
      await qc.invalidateQueries({ queryKey: ['dashboard'] });
      setSubject(''); setDescription(''); setPriority('MEDIUM'); setRelated('');
      onClose();
    },
  });

  function submit(e: FormEvent) { e.preventDefault(); mutation.mutate(); }

  return (
    <Modal
      open={open}
      onClose={() => { mutation.reset(); onClose(); }}
      title="Raise a ticket"
      description="You'll be recorded as the reporter."
      busy={mutation.isPending}
    >
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label className="block text-xs font-medium text-slate-700">Subject</label>
          <Input required maxLength={200} value={subject} onChange={e => setSubject(e.target.value)}
            placeholder="Short summary" className="mt-1" autoFocus />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-700">Description</label>
          <Textarea required value={description} onChange={e => setDescription(e.target.value)}
            placeholder="What's happening, what have you tried, is anything blocking you?" className="mt-1" rows={5} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-700">Priority</label>
            <Select value={priority} onChange={e => setPriority(e.target.value as TicketPriority)} className="mt-1">
              {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
            </Select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-700">Related asset ID (optional)</label>
            <Input type="number" value={relatedAssetId} onChange={e => setRelated(e.target.value)}
              placeholder="e.g. 1" className="mt-1" />
          </div>
        </div>

        {mutation.isError && <ErrorState error={mutation.error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose} disabled={mutation.isPending}>Cancel</Button>
          <Button type="submit" loading={mutation.isPending}>Create ticket</Button>
        </div>
      </form>
    </Modal>
  );
}
