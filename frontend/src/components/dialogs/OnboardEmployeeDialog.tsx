import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { peopleApi } from '../../api/people';
import { employeesApi } from '../../api/employees';
import { departmentsApi } from '../../api/departments';
import { FormDialog } from '../form/FormDialog';
import { FormField } from '../form/FormField';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { opt, todayIso } from '../../lib/dates';

const EMP_PATTERN = '^[A-Z0-9-]+$';   // mirrors EmployeeCreateRequest.employeeNumber

/**
 * Onboarding = Person (who) + Employee (employment record). The API keeps them separate
 * because not every person is an employee (contractors, external reporters).
 * Two calls; the created personId is remembered so a retry after a failed second
 * call doesn't create a duplicate person.
 */
export function OnboardEmployeeDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const qc = useQueryClient();
  const [mode, setMode] = useState<'new' | 'existing'>('new');
  const [existingPersonId, setExistingPersonId] = useState<number | ''>('');
  const [createdPersonId, setCreatedPersonId] = useState<number | null>(null);
  const [firstName, setFirst] = useState('');
  const [lastName, setLast] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [employeeNumber, setNumber] = useState('');
  const [departmentId, setDepartmentId] = useState<number | ''>('');
  const [jobTitle, setJobTitle] = useState('');
  const [hireDate, setHireDate] = useState(todayIso());

  const people = useQuery({ queryKey: ['people', 'all'], queryFn: () => peopleApi.list({ size: 200 }), enabled: open && mode === 'existing' });
  const departments = useQuery({ queryKey: ['departments', 'all'], queryFn: () => departmentsApi.list({ size: 200 }), enabled: open });

  const mutation = useMutation({
    mutationFn: async () => {
      let personId: number;
      if (mode === 'existing') personId = Number(existingPersonId);
      else if (createdPersonId != null) personId = createdPersonId;
      else {
        const p = await peopleApi.create({ firstName: firstName.trim(), lastName: lastName.trim(), email: email.trim(), phone: opt(phone) });
        setCreatedPersonId(p.id);
        personId = p.id;
      }
      return employeesApi.create({
        personId, employeeNumber: employeeNumber.trim().toUpperCase(), departmentId: Number(departmentId),
        jobTitle: opt(jobTitle), hireDate,
      });
    },
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['employees'] });
      await qc.invalidateQueries({ queryKey: ['people'] });
      await qc.invalidateQueries({ queryKey: ['dashboard'] });
      close();
    },
  });

  function close() {
    mutation.reset();
    setMode('new'); setExistingPersonId(''); setCreatedPersonId(null);
    setFirst(''); setLast(''); setEmail(''); setPhone('');
    setNumber(''); setDepartmentId(''); setJobTitle(''); setHireDate(todayIso());
    onClose();
  }

  const personOk = mode === 'existing' ? existingPersonId !== ''
    : createdPersonId != null || (firstName.trim() !== '' && lastName.trim() !== '' && email.trim() !== '');
  const canSubmit = personOk && employeeNumber.trim() !== '' && departmentId !== '' && hireDate !== '';

  return (
    <FormDialog open={open} onClose={close} title="Onboard an employee"
      description="Creates the person (if new) and their employment record."
      submitLabel="Onboard" onSubmit={() => mutation.mutate()} busy={mutation.isPending}
      error={mutation.error} canSubmit={canSubmit}>
      <FormField label="Person">
        {id => (
          <Select id={id} value={mode} disabled={createdPersonId != null}
            onChange={e => setMode(e.target.value as 'new' | 'existing')}>
            <option value="new">New person</option>
            <option value="existing">Existing person (e.g. former contractor)</option>
          </Select>
        )}
      </FormField>
      {mode === 'existing' ? (
        <FormField label="Choose person" required>
          {id => (
            <Select id={id} required value={existingPersonId} onChange={e => setExistingPersonId(e.target.value === '' ? '' : Number(e.target.value))}>
              <option value="">Choose…</option>
              {people.data?.content.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName} — {p.email}</option>)}
            </Select>
          )}
        </FormField>
      ) : createdPersonId != null ? (
        <p className="rounded-md bg-slate-50 p-2 text-xs text-slate-600">
          Person {firstName} {lastName} was created; fix the employment details below and retry.
        </p>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3">
            <FormField label="First name" required>
              {id => <Input id={id} required maxLength={80} value={firstName} onChange={e => setFirst(e.target.value)} autoFocus />}
            </FormField>
            <FormField label="Last name" required>
              {id => <Input id={id} required maxLength={80} value={lastName} onChange={e => setLast(e.target.value)} />}
            </FormField>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <FormField label="Email" required>
              {id => <Input id={id} type="email" required maxLength={160} value={email} onChange={e => setEmail(e.target.value)} />}
            </FormField>
            <FormField label="Phone">
              {id => <Input id={id} maxLength={40} value={phone} onChange={e => setPhone(e.target.value)} />}
            </FormField>
          </div>
        </>
      )}
      <hr className="border-slate-200" />
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Employee number" required hint="Uppercase letters, digits, dashes">
          {id => <Input id={id} required maxLength={30} pattern={EMP_PATTERN} value={employeeNumber}
            onChange={e => setNumber(e.target.value.toUpperCase())} placeholder="EMP-0123" />}
        </FormField>
        <FormField label="Department" required>
          {id => (
            <Select id={id} required value={departmentId} onChange={e => setDepartmentId(e.target.value === '' ? '' : Number(e.target.value))}>
              <option value="">Choose…</option>
              {departments.data?.content.map(d => <option key={d.id} value={d.id}>{d.code} — {d.name}</option>)}
            </Select>
          )}
        </FormField>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <FormField label="Job title">
          {id => <Input id={id} maxLength={120} value={jobTitle} onChange={e => setJobTitle(e.target.value)} />}
        </FormField>
        <FormField label="Hire date" required>
          {id => <Input id={id} type="date" required value={hireDate} onChange={e => setHireDate(e.target.value)} />}
        </FormField>
      </div>
    </FormDialog>
  );
}
