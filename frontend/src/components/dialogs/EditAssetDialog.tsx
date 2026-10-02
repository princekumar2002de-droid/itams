import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { assetsApi } from '../../api/assets';
import type { Asset } from '../../api/types';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Textarea } from '../ui/Textarea';

/** Only warranty and notes are editable: tag, model, serial and purchase data are the asset's identity. */
export function EditAssetDialog({ open, onClose, asset }: { open: boolean; onClose: () => void; asset: Asset }) {
  const qc = useQueryClient();
  const [warrantyEndsOn, setWarranty] = useState(asset.warrantyEndsOn ?? '');
  const [notes, setNotes] = useState(asset.notes ?? '');
  useEffect(() => { if (open) { setWarranty(asset.warrantyEndsOn ?? ''); setNotes(asset.notes ?? ''); } }, [open, asset]);

  const mutation = useMutation({
    mutationFn: () => assetsApi.update(asset.id, {
      warrantyEndsOn: warrantyEndsOn || undefined,
      notes,
    }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['asset', asset.id] });
      await qc.invalidateQueries({ queryKey: ['assets'] });
      mutation.reset(); onClose();
    },
  });

  return (
    <FormDialog open={open} onClose={() => { mutation.reset(); onClose(); }} title={`Edit ${asset.assetTag}`}
      description="Tag, model, serial number and purchase data cannot be changed."
      submitLabel="Save" onSubmit={() => mutation.mutate()} busy={mutation.isPending} error={mutation.error}>
      <FormField label="Warranty ends">
        {id => <Input id={id} type="date" value={warrantyEndsOn} onChange={e => setWarranty(e.target.value)} />}
      </FormField>
      <FormField label="Notes">
        {id => <Textarea id={id} rows={4} value={notes} onChange={e => setNotes(e.target.value)} />}
      </FormField>
    </FormDialog>
  );
}
