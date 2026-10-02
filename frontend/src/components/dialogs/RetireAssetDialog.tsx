import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { assetsApi } from '../../api/assets';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Textarea } from '../ui/Textarea';

export function RetireAssetDialog({ open, onClose, assetId, assetTag }: {
  open: boolean; onClose: () => void; assetId: number; assetTag: string;
}) {
  const qc = useQueryClient();
  const [reason, setReason] = useState('');
  const mutation = useMutation({
    mutationFn: () => assetsApi.retire(assetId, reason.trim()),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['asset', assetId] });
      await qc.invalidateQueries({ queryKey: ['assets'] });
      await qc.invalidateQueries({ queryKey: ['dashboard'] });
      setReason(''); mutation.reset(); onClose();
    },
  });
  return (
    <FormDialog open={open} onClose={() => { mutation.reset(); onClose(); }} title={`Retire ${assetTag}`}
      description="Retirement is final: the asset can no longer be assigned or edited."
      submitLabel="Retire asset" danger onSubmit={() => mutation.mutate()} busy={mutation.isPending}
      error={mutation.error} canSubmit={reason.trim() !== ''}>
      <FormField label="Reason" required>
        {id => <Textarea id={id} required maxLength={255} rows={3} value={reason}
          onChange={e => setReason(e.target.value)} placeholder="e.g. End of life, beyond economic repair" autoFocus />}
      </FormField>
    </FormDialog>
  );
}
