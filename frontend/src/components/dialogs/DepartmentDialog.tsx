import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { departmentsApi } from '../../api/departments';
import { peopleApi } from '../../api/people';
import type { Department } from '../../api/types';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';

const CODE_PATTERN = '^[A-Z0-9][A-Z0-9_-]*$';   // mirrors DepartmentCreateRequest.code

/** Create (department undefined) or edit. The code is immutable after creation (printed on labels). */
export function DepartmentDialog({ open, onClose, department }: { open: boolean; onClose: () => void; department?: Department }) {
  const qc = useQueryClient();
  const editing = department !== undefined;
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [parentId, setParentId] = useState<number | ''>('');
  const [managerId, setManagerId] = useState<number | ''>('');

  useEffect(() => {
    if (!open) return;
    setCode(department?.code ?? ''); setName(department?.name ?? '');
    setParentId(department?.parentDepartmentId ?? ''); setManagerId(department?.managerPersonId ?? '');
  }, [open, department]);

  const departments = useQuery({ queryKey: ['departments', 'all'], queryFn: () => departmentsApi.list({ size: 200 }), enabled: open });
  const people = useQuery({ queryKey: ['people', 'all'], queryFn: () => peopleApi.list({ size: 200 }), enabled: open });

  const mutation = useMutation({
    mutationFn: () => editing
      ? departmentsApi.update(department!.id, {
          name: name.trim(),
          parentDepartmentId: parentId === '' ? undefined : parentId,
          managerPersonId: managerId === '' ? undefined : managerId,
        })
      : departmentsApi.create({
          code: code.trim().toUpperCase(), name: name.trim(),
          parentDepartmentId: parentId === '' ? undefined : parentId,
          managerPersonId: managerId === '' ? undefined : managerId,
        }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['departments'] });
      mutation.reset(); onClose();
    },
  });

  return (
    <FormDialog open={open} onClose={() => { mutation.reset(); onClose(); }}
      title={editing ? `Edit ${department!.code}` : 'New department'}
      submitLabel={editing ? 'Save' : 'Create department'} onSubmit={() => mutation.mutate()}
      busy={mutation.isPending} error={mutation.error}
      canSubmit={name.trim() !== '' && (editing || code.trim() !== '')}>
      <div className="grid grid-cols-3 gap-3">
        <FormField label="Code" required={!editing} hint={editing ? 'Cannot be changed' : 'e.g. FIN, IT-OPS'}>
          {id => <Input id={id} required={!editing} disabled={editing} maxLength={30} pattern={CODE_PATTERN}
            value={code} onChange={e => setCode(e.target.value.toUpperCase())} autoFocus={!editing} />}
        </FormField>
        <FormField label="Name" required className="col-span-2">
          {id => <Input id={id} required maxLength={160} value={name} onChange={e => setName(e.target.value)} />}
        </FormField>
      </div>
      <FormField label="Parent department">
        {id => (
          <Select id={id} value={parentId} onChange={e => setParentId(e.target.value === '' ? '' : Number(e.target.value))}>
            <option value="">— none (top level) —</option>
            {departments.data?.content.filter(d => d.id !== department?.id).map(d => (
              <option key={d.id} value={d.id}>{d.code} — {d.name}</option>
            ))}
          </Select>
        )}
      </FormField>
      <FormField label="Manager">
        {id => (
          <Select id={id} value={managerId} onChange={e => setManagerId(e.target.value === '' ? '' : Number(e.target.value))}>
            <option value="">— none —</option>
            {people.data?.content.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
          </Select>
        )}
      </FormField>
    </FormDialog>
  );
}
