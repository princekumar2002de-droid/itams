import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { assignmentsApi } from '../api/assignments';
import { PageHeader } from '../components/ui/PageHeader';
import { Card } from '../components/ui/Card';
import { Select } from '../components/ui/Select';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { TableSkeleton } from '../components/ui/Skeleton';
import { Pagination } from '../components/ui/Pagination';
import { fmtDate, fmtDateTime } from '../lib/format';

const SIZE = 20;

export function AssignmentsPage() {
  const [onlyOpen, setOnlyOpen] = useState<'all' | 'open' | 'closed'>('all');
  const [page, setPage] = useState(0);

  const list = useQuery({
    queryKey: ['assignments', { onlyOpen, page }],
    queryFn: () => assignmentsApi.list({
      onlyOpen: onlyOpen === 'open' ? true : undefined,
      page, size: SIZE,
    }),
    select: raw => {
      // Client-side filter for 'closed' since backend only supports onlyOpen=true.
      if (onlyOpen !== 'closed') return raw;
      return { ...raw, content: raw.content.filter(a => !a.open) };
    },
  });

  return (
    <>
      <PageHeader title="Assignments" description="Who has which asset, past and present." />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3">
          <Select
            value={onlyOpen}
            onChange={e => { setOnlyOpen(e.target.value as typeof onlyOpen); setPage(0); }}
            className="w-full sm:w-48"
          >
            <option value="all">All assignments</option>
            <option value="open">Open only</option>
            <option value="closed">Closed only (this page)</option>
          </Select>
        </div>

        {list.isLoading ? <TableSkeleton /> :
         list.isError ? <div className="p-4"><ErrorState error={list.error} /></div> :
         list.data && list.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5">Asset</th>
                    <th className="px-4 py-2.5">Assignee</th>
                    <th className="px-4 py-2.5">Assigned</th>
                    <th className="px-4 py-2.5">Expected return</th>
                    <th className="px-4 py-2.5">Returned</th>
                    <th className="px-4 py-2.5">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {list.data.content.map(a => (
                    <tr key={a.id} className="hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-mono text-xs">
                        <Link to={`/assets/${a.assetId}`} className="text-brand-700 hover:underline">{a.assetTag}</Link>
                      </td>
                      <td className="px-4 py-2.5">{a.assigneeName}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDateTime(a.assignedAt)}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDate(a.expectedReturnOn)}</td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDateTime(a.actualReturnAt)}</td>
                      <td className="px-4 py-2.5">
                        {a.open ? <Badge tone="green">Open</Badge> : <Badge tone="gray">Closed</Badge>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={list.data.page}
              totalPages={list.data.totalPages}
              first={list.data.first}
              last={list.data.last}
              totalElements={list.data.totalElements}
              size={list.data.size}
              onChange={setPage}
            />
          </>
        ) : (
          <EmptyState title="No assignments" />
        )}
      </Card>
    </>
  );
}
