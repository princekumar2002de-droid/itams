import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { licensesApi } from '../../api/licenses';
import type { LicenseType } from '../../api/types';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { opt, todayIso } from '../../lib/dates';

const TYPES: { value: LicenseType; label: string }[] = [
  { value: 'PER_SEAT', label: 'Per seat (named user)' },
  { value: 'PER_DEVICE', label: 'Per device' },
  { value: 'SITE', label: 'Site licence' },
  { value: 'SUBSCRIPTION', label: 'Subscription' },
];

export function CreateLicenseDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [vendor, setVendor] = useState('');
  const [productName, setProduct] = useState('');
  const [productVersion, setVersion] = useState('');
  const [licenseReference, setReference] = useState('');
  const [licenseType, setType] = useState<LicenseType>('PER_SEAT');
  const [seatsTotal, setSeats] = useState('10');
  const [purchaseDate, setPurchase] = useState(todayIso());
  const [expiresOn, setExpires] = useState('');
  const [cost, setCost] = useState('');
  const [procurementRef, setProcurement] = useState('');

  const mutation = useMutation({
    mutationFn: () => licensesApi.create({
      vendor: vendor.trim(), productName: productName.trim(), productVersion: opt(productVersion),
      licenseReference: licenseReference.trim(), licenseType, seatsTotal: Number(seatsTotal),
      purchaseDate, expiresOn: opt(expiresOn), cost: Number(cost), procurementRef: opt(procurementRef),
    }),
    onSuccess: async (license) => {
      await qc.invalidateQueries({ queryKey: ['licenses'] });
      await qc.invalidateQueries({ queryKey: ['dashboard'] });
      close();
      navigate(`/licenses/${license.id}`);
    },
  });

  function close() {
    mutation.reset();
    setVendor(''); setProduct(''); setVersion(''); setReference(''); setType('PER_SEAT'); setSeats('10');
    setPurchase(todayIso()); setExpires(''); setCost(''); setProcurement('');
    onClose();
  }

  const canSubmit = vendor.trim() !== '' && productName.trim() !== '' && licenseReference.trim() !== ''
    && Number(seatsTotal) > 0 && purchaseDate !== '' && cost !== '' && Number(cost) >= 0;

  return (
    <FormDialog open={open} onClose={close} title="New software licence"
      description="The product is created automatically if it doesn't exist yet."
      submitLabel="Create licence" onSubmit={() => mutation.mutate()} busy={mutation.isPending}
      error={mutation.error} canSubmit={canSubmit}>
      <div className="grid grid-cols-3 gap-3">
        <FormField label="Vendor" required>
          {id => <Input id={id} required maxLength={120} value={vendor} onChange={e => setVendor(e.target.value)} placeholder="Microsoft" autoFocus />}
        </FormField>
        <FormField label="Product" required>
          {id => <Input id={id} required maxLength={160} value={productName} onChange={e => setProduct(e.target.value)} placeholder="Office 365" />}
        </FormField>
        <FormField label="Version">
          {id => <Input id={id} maxLength={60} value={productVersion} onChange={e => setVersion(e.target.value)} placeholder="optional" />}
        </FormField>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Licence reference" required hint="Contract or key reference">
          {id => <Input id={id} required maxLength={120} value={licenseReference} onChange={e => setReference(e.target.value)} />}
        </FormField>
        <FormField label="Type" required>
          {id => (
            <Select id={id} value={licenseType} onChange={e => setType(e.target.value as LicenseType)}>
              {TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
            </Select>
          )}
        </FormField>
      </div>
      <div className="grid grid-cols-3 gap-3">
        <FormField label="Seats" required>
          {id => <Input id={id} type="number" required min="1" step="1" value={seatsTotal} onChange={e => setSeats(e.target.value)} />}
        </FormField>
        <FormField label="Cost (EUR)" required>
          {id => <Input id={id} type="number" required min="0" step="0.01" value={cost} onChange={e => setCost(e.target.value)} />}
        </FormField>
        <FormField label="Procurement ref">
          {id => <Input id={id} maxLength={120} value={procurementRef} onChange={e => setProcurement(e.target.value)} placeholder="PO-…" />}
        </FormField>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Purchase date" required>
          {id => <Input id={id} type="date" required max={todayIso()} value={purchaseDate} onChange={e => setPurchase(e.target.value)} />}
        </FormField>
        <FormField label="Expires on">
          {id => <Input id={id} type="date" min={todayIso()} value={expiresOn} onChange={e => setExpires(e.target.value)} />}
        </FormField>
      </div>
    </FormDialog>
  );
}
