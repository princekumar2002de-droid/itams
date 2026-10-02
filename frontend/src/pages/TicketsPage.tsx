import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { ticketsApi } from '../api/tickets';
import type { TicketPriority, TicketStatus } from '../api/types';
import { PageHeader } from '../components/ui/PageHeader';
import { Card } from '../components/ui/Card';
import { SearchInput } from '../components/ui/SearchInput';
import { Select } from '../components/ui/Select';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { TableSkeleton } from '../components/ui/Skeleton';
import { Pagination } from '../components/ui/Pagination';
import { CreateTicketDialog } from '../components/dialogs/CreateTicketDialog';
import { fmtDateTime } from '../lib/format';
import { Link } from 'react-router-dom';
import { usePermissions } from '../auth/permissions';
import { TicketPriorityBadge as PriorityBadge, TicketStatusBadge as StatusBadge } from '../components/ui/TicketBadges';

const SIZE = 20;
const STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'WAITING', 'RESOLVED', 'CLOSED'];
const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function TicketsPage() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<TicketStatus | ''>('');
  const [priority, setPriority] = useState<TicketPriority | ''>('');
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const { canManage } = usePermissions();

  const list = useQuery({
    queryKey: ['tickets', { q, status, priority, page }],
    queryFn: () => ticketsApi.list({
      q: q || undefined,
      status: status === '' ? undefined : status,
      priority: priority === '' ? undefined : priority,
      page, size: SIZE,
    }),
  });

  return (
    <>
      <PageHeader
        title="Support tickets"
        description={canManage ? 'Every ticket raised in the system.' : 'Tickets you raised or that are assigned to you.'}
        actions={
          <Button onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4" /> Raise ticket
          </Button>
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3">
          <SearchInput value={q} onChange={v => { setQ(v); setPage(0); }}
            placeholder="Search subject or number…" className="w-full sm:w-64" />
          <Select value={status} onChange={e => { setStatus(e.target.value as TicketStatus | ''); setPage(0); }}
            className="w-full sm:w-40">
            <option value="">All statuses</option>
            {STATUSES.map(s => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
          </Select>
          <Select value={priority} onChange={e => { setPriority(e.target.value as TicketPriority | ''); setPage(0); }}
            className="w-full sm:w-40">
            <option value="">All priorities</option>
            {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
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
                    <th className="px-4 py-2.5">Number</th>
                    <th className="px-4 py-2.5">Subject</th>
                    <th className="px-4 py-2.5">Reporter</th>
                    <th className="px-4 py-2.5">Priority</th>
                    <th className="px-4 py-2.5">Status</th>
                    <th className="px-4 py-2.5">Created</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {list.data.content.map(t => (
                    <tr key={t.id} className="hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-mono text-xs">{t.ticketNumber}</td>
                      <td className="px-4 py-2.5">
                        <Link to={`/tickets/${t.id}`} className="block max-w-md truncate font-medium text-brand-700 hover:underline" title={t.subject}>{t.subject}</Link>
                        {t.relatedAssetTag && (
                          <div className="text-xs text-slate-500">re: {t.relatedAssetTag}</div>
                        )}
                      </td>
                      <td className="px-4 py-2.5 text-slate-700">{t.reporterName}</td>
                      <td className="px-4 py-2.5"><PriorityBadge priority={t.priority} /></td>
                      <td className="px-4 py-2.5"><StatusBadge status={t.status} /></td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDateTime(t.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={list.data.page} totalPages={list.data.totalPages}
              first={list.data.first} last={list.data.last}
              totalElements={list.data.totalElements} size={list.data.size}
              onChange={setPage} />
          </>
        ) : (
          <EmptyState title="No tickets match your filters"
            description="Try clearing the search / filters, or raise a new ticket." />
        )}
      </Card>

      <CreateTicketDialog open={dialogOpen} onClose={() => setDialogOpen(false)} />
    </>
  );
}
