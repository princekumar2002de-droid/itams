import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { assetsApi } from '../../api/assets';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Textarea } from '../ui/Textarea';
import { CreateAssetModelDialog } from './CreateAssetModelDialog';
import { opt, todayIso } from '../../lib/dates';

const TAG_PATTERN = '^[A-Z0-9-]+$';   // mirrors AssetCreateRequest.assetTag

export function CreateAssetDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [assetTag, setTag] = useState('');
  const [modelId, setModelId] = useState<number | ''>('');
  const [serialNumber, setSerial] = useState('');
  const [purchaseDate, setPurchaseDate] = useState(todayIso());
  const [purchasePrice, setPrice] = useState('');
  const [warrantyEndsOn, setWarranty] = useState('');
  const [notes, setNotes] = useState('');
  const [modelDialog, setModelDialog] = useState(false);

  const models = useQuery({
    queryKey: ['asset-models', 'all'],
    queryFn: () => assetsApi.models({ size: 200 }),
    enabled: open,
  });

  const mutation = useMutation({
    mutationFn: () => assetsApi.create({
      assetTag: assetTag.trim().toUpperCase(),
      modelId: Number(modelId),
      serialNumber: serialNumber.trim(),
      purchaseDate,
      purchasePrice: Number(purchasePrice),
      warrantyEndsOn: opt(warrantyEndsOn),
      notes: opt(notes),
    }),
    onSuccess: async (asset) => {
      await qc.invalidateQueries({ queryKey: ['assets'] });
      await qc.invalidateQueries({ queryKey: ['dashboard'] });
      close();
      navigate(`/assets/${asset.id}`);
    },
  });

  function close() {
    mutation.reset();
    setTag(''); setModelId(''); setSerial(''); setPurchaseDate(todayIso());
    setPrice(''); setWarranty(''); setNotes('');
    onClose();
  }

  const canSubmit = assetTag.trim() !== '' && modelId !== '' && serialNumber.trim() !== ''
    && purchaseDate !== '' && purchasePrice !== '' && Number(purchasePrice) >= 0;

  return (
    <>
      <FormDialog open={open && !modelDialog} onClose={close} title="Register a new asset"
        description="The asset starts in stock and can then be assigned."
        submitLabel="Create asset" onSubmit={() => mutation.mutate()}
        busy={mutation.isPending} error={mutation.error} canSubmit={canSubmit}>
        <div className="grid grid-cols-2 gap-3">
          <FormField label="Asset tag" required hint="Uppercase letters, digits, dashes">
            {id => <Input id={id} required maxLength={30} pattern={TAG_PATTERN} value={assetTag}
              onChange={e => setTag(e.target.value.toUpperCase())} placeholder="LAP-0042" autoFocus />}
          </FormField>
          <FormField label="Serial number" required>
            {id => <Input id={id} required maxLength={80} value={serialNumber} onChange={e => setSerial(e.target.value)} />}
          </FormField>
        </div>
        <FormField label="Model" required>
          {id => (
            <div className="flex gap-2">
              <Select id={id} required value={modelId} className="flex-1"
                onChange={e => setModelId(e.target.value === '' ? '' : Number(e.target.value))}>
                <option value="">Choose a model…</option>
                {models.data?.content.map(m => (
                  <option key={m.id} value={m.id}>{m.manufacturer} {m.modelName} ({m.categoryCode})</option>
                ))}
              </Select>
              <button type="button" onClick={() => setModelDialog(true)}
                className="whitespace-nowrap text-sm font-medium text-brand-700 hover:underline">
                New model
              </button>
            </div>
          )}
        </FormField>
        <div className="grid grid-cols-2 gap-3">
          <FormField label="Purchase date" required>
            {id => <Input id={id} type="date" required max={todayIso()} value={purchaseDate} onChange={e => setPurchaseDate(e.target.value)} />}
          </FormField>
          <FormField label="Purchase price (EUR)" required>
            {id => <Input id={id} type="number" required min="0" step="0.01" value={purchasePrice} onChange={e => setPrice(e.target.value)} />}
          </FormField>
        </div>
        <FormField label="Warranty ends">
          {id => <Input id={id} type="date" value={warrantyEndsOn} onChange={e => setWarranty(e.target.value)} />}
        </FormField>
        <FormField label="Notes">
          {id => <Textarea id={id} rows={2} value={notes} onChange={e => setNotes(e.target.value)} />}
        </FormField>
      </FormDialog>

      <CreateAssetModelDialog open={open && modelDialog} onClose={() => setModelDialog(false)}
        onCreated={m => { setModelId(m.id); setModelDialog(false); }} />
    </>
  );
}
