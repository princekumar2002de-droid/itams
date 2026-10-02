import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { departmentsApi } from '../api/departments';
import type { Department } from '../api/types';
import { usePermissions } from '../auth/permissions';
import { PageHeader } from '../components/ui/PageHeader';
import { Card } from '../components/ui/Card';
import { SearchInput } from '../components/ui/SearchInput';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { TableSkeleton } from '../components/ui/Skeleton';
import { Pagination } from '../components/ui/Pagination';
import { DepartmentDialog } from '../components/dialogs/DepartmentDialog';
import { ConfirmDialog } from '../components/form/ConfirmDialog';

export function DepartmentsPage() {
  const qc = useQueryClient();
  const { isAdmin } = usePermissions();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Department | undefined>();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleting, setDeleting] = useState<Department | null>(null);

  const list = useQuery({ queryKey: ['departments', { q, page }], queryFn: () => departmentsApi.list({ q: q || undefined, page, size: 20 }) });
  const all = useQuery({ queryKey: ['departments', 'all'], queryFn: () => departmentsApi.list({ size: 200 }) });
  const codeOf = (id: number | null) => (id == null ? '—' : all.data?.content.find(d => d.id === id)?.code ?? `#${id}`);

  const remove = useMutation({
    mutationFn: (id: number) => departmentsApi.remove(id),
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: ['departments'] }); setDeleting(null); },
  });

  return (
    <>
      <PageHeader title="Departments" description="Organisational units that employees and assets roll up to."
        actions={isAdmin && <Button onClick={() => { setEditing(undefined); setDialogOpen(true); }}><Plus className="h-4 w-4" /> New department</Button>} />
      <Card>
        <div className="border-b border-slate-200 p-3">
          <SearchInput value={q} onChange={v => { setQ(v); setPage(0); }} placeholder="Search code or name…" className="w-full sm:w-72" />
        </div>
        {list.isLoading ? <TableSkeleton /> :
         list.isError ? <div className="p-4"><ErrorState error={list.error} /></div> :
         list.data && list.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr><th className="px-4 py-2.5">Code</th><th className="px-4 py-2.5">Name</th><th className="px-4 py-2.5">Parent</th><th className="px-4 py-2.5" /></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {list.data.content.map(d => (
                    <tr key={d.id} className="hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-mono text-xs">{d.code}</td>
                      <td className="px-4 py-2.5 font-medium text-slate-900">{d.name}</td>
                      <td className="px-4 py-2.5 font-mono text-xs text-slate-600">{codeOf(d.parentDepartmentId)}</td>
                      <td className="px-4 py-2.5 text-right">
                        {isAdmin && (
                          <div className="flex justify-end gap-2">
                            <Button size="sm" variant="secondary" onClick={() => { setEditing(d); setDialogOpen(true); }}>Edit</Button>
                            <Button size="sm" variant="ghost" onClick={() => { remove.reset(); setDeleting(d); }}>Delete</Button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={list.data.page} totalPages={list.data.totalPages} first={list.data.first} last={list.data.last}
              totalElements={list.data.totalElements} size={list.data.size} onChange={setPage} />
          </>
        ) : <EmptyState title="No departments" description={isAdmin ? 'Create the first department to start onboarding employees.' : undefined} />}
      </Card>

      <DepartmentDialog open={dialogOpen} onClose={() => setDialogOpen(false)} department={editing} />
      <ConfirmDialog open={deleting != null} onClose={() => setDeleting(null)}
        title={`Delete ${deleting?.code ?? ''}?`}
        message="The department is soft-deleted: it disappears from lists but stays in the database for history. Move its employees to another department first."
        onConfirm={() => deleting && remove.mutate(deleting.id)} busy={remove.isPending} error={remove.error} />
    </>
  );
}
