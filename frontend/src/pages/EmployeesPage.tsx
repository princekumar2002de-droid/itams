import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { usePermissions } from '../auth/permissions';
import { Button } from '../components/ui/Button';
import { OnboardEmployeeDialog } from '../components/dialogs/OnboardEmployeeDialog';
import { EditEmployeeDialog } from '../components/dialogs/EditEmployeeDialog';
import type { Employee } from '../api/types';
import { employeesApi } from '../api/employees';
import { departmentsApi } from '../api/departments';
import { PageHeader } from '../components/ui/PageHeader';
import { SearchInput } from '../components/ui/SearchInput';
import { Select } from '../components/ui/Select';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { TableSkeleton } from '../components/ui/Skeleton';
import { Pagination } from '../components/ui/Pagination';
import { fmtDate } from '../lib/format';

const SIZE = 20;

export function EmployeesPage() {
  const [q, setQ] = useState('');
  const [departmentId, setDepartmentId] = useState<number | ''>('');
  const [page, setPage] = useState(0);
  const [onboardOpen, setOnboardOpen] = useState(false);
  const [editing, setEditing] = useState<Employee | null>(null);
  const { isAdmin } = usePermissions();

  // Department dropdown data
  const depts = useQuery({
    queryKey: ['departments', 'all'],
    queryFn: () => departmentsApi.list({ size: 100 }),
  });

  const employees = useQuery({
    queryKey: ['employees', { q, departmentId, page }],
    queryFn: () => employeesApi.list({
      q: q || undefined,
      departmentId: departmentId === '' ? undefined : departmentId,
      page, size: SIZE,
    }),
  });

  return (
    <>
      <PageHeader title="Employees" description="Directory of employment records."
        actions={isAdmin && <Button onClick={() => setOnboardOpen(true)}><Plus className="h-4 w-4" /> Onboard employee</Button>} />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3">
          <SearchInput
            value={q}
            onChange={v => { setQ(v); setPage(0); }}
            placeholder="Search name, number, email…"
            className="w-full sm:w-72"
          />
          <Select
            value={departmentId}
            onChange={e => { setDepartmentId(e.target.value === '' ? '' : Number(e.target.value)); setPage(0); }}
            className="w-full sm:w-56"
          >
            <option value="">All departments</option>
            {depts.data?.content.map(d => (
              <option key={d.id} value={d.id}>{d.code} — {d.name}</option>
            ))}
          </Select>
        </div>

        {employees.isLoading ? <TableSkeleton /> :
         employees.isError ? <div className="p-4"><ErrorState error={employees.error} /></div> :
         employees.data && employees.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5">Employee #</th>
                    <th className="px-4 py-2.5">Name</th>
                    <th className="px-4 py-2.5">Department</th>
                    <th className="px-4 py-2.5">Job Title</th>
                    <th className="px-4 py-2.5">Hire Date</th>
                    <th className="px-4 py-2.5">Status</th>
                    {isAdmin && <th className="px-4 py-2.5" />}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {employees.data.content.map(e => (
                    <tr key={e.id} className="hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-mono text-xs">{e.employeeNumber}</td>
                      <td className="px-4 py-2.5">
                        <div className="font-medium text-slate-900">{e.firstName} {e.lastName}</div>
                        <div className="text-xs text-slate-500">{e.email}</div>
                      </td>
                      <td className="px-4 py-2.5">{e.departmentCode}</td>
                      <td className="px-4 py-2.5 text-slate-700">{e.jobTitle ?? '—'}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDate(e.hireDate)}</td>
                      <td className="px-4 py-2.5">
                        <Badge tone={e.employmentStatus === 'ACTIVE' ? 'green' :
                                     e.employmentStatus === 'ON_LEAVE' ? 'amber' : 'slate'}>
                          {e.employmentStatus.replace('_', ' ')}
                        </Badge>
                      </td>
                      {isAdmin && (
                        <td className="px-4 py-2.5 text-right">
                          <Button size="sm" variant="secondary" onClick={() => setEditing(e)}>Edit</Button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={employees.data.page}
              totalPages={employees.data.totalPages}
              first={employees.data.first}
              last={employees.data.last}
              totalElements={employees.data.totalElements}
              size={employees.data.size}
              onChange={setPage}
            />
          </>
        ) : (
          <EmptyState
            title="No employees match your filters"
            description="Try clearing the search or department filter."
          />
        )}
      </Card>
      <OnboardEmployeeDialog open={onboardOpen} onClose={() => setOnboardOpen(false)} />
      {editing && <EditEmployeeDialog open onClose={() => setEditing(null)} employee={editing} />}
    </>
  );
}
