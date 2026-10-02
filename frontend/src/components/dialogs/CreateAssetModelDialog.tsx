import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { assetsApi } from '../../api/assets';
import type { AssetModel } from '../../api/types';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';

export function CreateAssetModelDialog({ open, onClose, onCreated }: {
  open: boolean; onClose: () => void; onCreated: (m: AssetModel) => void;
}) {
  const qc = useQueryClient();
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [manufacturer, setManufacturer] = useState('');
  const [modelName, setModelName] = useState('');

  const categories = useQuery({ queryKey: ['asset-categories'], queryFn: () => assetsApi.categories(), enabled: open });

  const mutation = useMutation({
    mutationFn: () => assetsApi.createModel({
      categoryId: Number(categoryId), manufacturer: manufacturer.trim(), modelName: modelName.trim(),
    }),
    onSuccess: async (m) => {
      await qc.invalidateQueries({ queryKey: ['asset-models'] });
      setCategoryId(''); setManufacturer(''); setModelName(''); mutation.reset();
      onCreated(m);
    },
  });

  return (
    <FormDialog open={open} onClose={() => { mutation.reset(); onClose(); }} title="New asset model"
      description="A model is the catalogue entry (e.g. Lenovo ThinkPad T14); assets are physical units of it."
      submitLabel="Create model" onSubmit={() => mutation.mutate()} busy={mutation.isPending}
      error={mutation.error} canSubmit={categoryId !== '' && manufacturer.trim() !== '' && modelName.trim() !== ''}>
      <FormField label="Category" required>
        {id => (
          <Select id={id} required value={categoryId}
            onChange={e => setCategoryId(e.target.value === '' ? '' : Number(e.target.value))}>
            <option value="">Choose…</option>
            {categories.data?.map(c => <option key={c.id} value={c.id}>{c.name} ({c.code})</option>)}
          </Select>
        )}
      </FormField>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Manufacturer" required>
          {id => <Input id={id} required maxLength={80} value={manufacturer} onChange={e => setManufacturer(e.target.value)} />}
        </FormField>
        <FormField label="Model name" required>
          {id => <Input id={id} required maxLength={160} value={modelName} onChange={e => setModelName(e.target.value)} />}
        </FormField>
      </div>
    </FormDialog>
  );
}
