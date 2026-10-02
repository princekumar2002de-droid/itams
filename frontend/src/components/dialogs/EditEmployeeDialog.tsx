import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { peopleApi } from '../../api/people';
import { employeesApi } from '../../api/employees';
import { departmentsApi } from '../../api/departments';
import type { Employee, EmploymentStatus } from '../../api/types';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';

const STATUSES: EmploymentStatus[] = ['ACTIVE', 'ON_LEAVE', 'LEFT'];

/** Edits contact details (Person) and employment details (Employee). Offboarding = status LEFT + end date. */
export function EditEmployeeDialog({ open, onClose, employee }: { open: boolean; onClose: () => void; employee: Employee }) {
  const qc = useQueryClient();
  const [firstName, setFirst] = useState(employee.firstName);
  const [lastName, setLast] = useState(employee.lastName);
  const [email, setEmail] = useState(employee.email);
  const [departmentId, setDepartmentId] = useState<number>(employee.departmentId);
  const [jobTitle, setJobTitle] = useState(employee.jobTitle ?? '');
  const [status, setStatus] = useState<EmploymentStatus>(employee.employmentStatus);
  const [endDate, setEndDate] = useState(employee.endDate ?? '');

  useEffect(() => {
    if (!open) return;
    setFirst(employee.firstName); setLast(employee.lastName); setEmail(employee.email);
    setDepartmentId(employee.departmentId); setJobTitle(employee.jobTitle ?? '');
    setStatus(employee.employmentStatus); setEndDate(employee.endDate ?? '');
  }, [open, employee]);

  const departments = useQuery({ queryKey: ['departments', 'all'], queryFn: () => departmentsApi.list({ size: 200 }), enabled: open });

  const mutation = useMutation({
    mutationFn: async () => {
      const personChanged = firstName !== employee.firstName || lastName !== employee.lastName || email !== employee.email;
      if (personChanged) {
        await peopleApi.update(employee.personId, { firstName: firstName.trim(), lastName: lastName.trim(), email: email.trim() });
      }
      return employeesApi.update(employee.id, {
        departmentId, jobTitle: jobTitle.trim(), employmentStatus: status, endDate: endDate || undefined,
      });
    },
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['employees'] });
      mutation.reset(); onClose();
    },
  });

  return (
    <FormDialog open={open} onClose={() => { mutation.reset(); onClose(); }}
      title={`Edit ${employee.firstName} ${employee.lastName}`} description={`Employee ${employee.employeeNumber}`}
      submitLabel="Save" onSubmit={() => mutation.mutate()} busy={mutation.isPending} error={mutation.error}
      canSubmit={firstName.trim() !== '' && lastName.trim() !== '' && email.trim() !== '' && (status !== 'LEFT' || endDate !== '')}>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="First name" required>
          {id => <Input id={id} required maxLength={80} value={firstName} onChange={e => setFirst(e.target.value)} />}
        </FormField>
        <FormField label="Last name" required>
          {id => <Input id={id} required maxLength={80} value={lastName} onChange={e => setLast(e.target.value)} />}
        </FormField>
      </div>
      <FormField label="Email" required>
        {id => <Input id={id} type="email" required maxLength={160} value={email} onChange={e => setEmail(e.target.value)} />}
      </FormField>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Department" required>
          {id => (
            <Select id={id} value={departmentId} onChange={e => setDepartmentId(Number(e.target.value))}>
              {departments.data?.content.map(d => <option key={d.id} value={d.id}>{d.code} — {d.name}</option>)}
            </Select>
          )}
        </FormField>
        <FormField label="Job title">
          {id => <Input id={id} maxLength={120} value={jobTitle} onChange={e => setJobTitle(e.target.value)} />}
        </FormField>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Employment status">
          {id => (
            <Select id={id} value={status} onChange={e => setStatus(e.target.value as EmploymentStatus)}>
              {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
            </Select>
          )}
        </FormField>
        <FormField label="End date" required={status === 'LEFT'} hint={status === 'LEFT' ? 'Required when offboarding' : undefined}>
          {id => <Input id={id} type="date" value={endDate} onChange={e => setEndDate(e.target.value)} />}
        </FormField>
      </div>
    </FormDialog>
  );
}
