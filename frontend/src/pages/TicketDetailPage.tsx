import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { ticketsApi } from '../api/tickets';
import type { TicketPriority, TicketStatus } from '../api/types';
import { usePermissions } from '../auth/permissions';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardBody, CardHeader } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Select } from '../components/ui/Select';
import { Textarea } from '../components/ui/Textarea';
import { Badge } from '../components/ui/Badge';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton } from '../components/ui/Skeleton';
import { TicketPriorityBadge, TicketStatusBadge } from '../components/ui/TicketBadges';
import { fmtDateTime } from '../lib/format';

/** Mirror of Ticket.ALLOWED_TRANSITIONS in the backend — only legal next steps are offered. */
export const TRANSITIONS: Record<TicketStatus, { to: TicketStatus; label: string }[]> = {
  OPEN:        [{ to: 'IN_PROGRESS', label: 'Start work' }, { to: 'CLOSED', label: 'Close' }],
  IN_PROGRESS: [{ to: 'WAITING', label: 'Waiting for reply' }, { to: 'RESOLVED', label: 'Resolve' }, { to: 'CLOSED', label: 'Close' }],
  WAITING:     [{ to: 'IN_PROGRESS', label: 'Resume' }, { to: 'RESOLVED', label: 'Resolve' }, { to: 'CLOSED', label: 'Close' }],
  RESOLVED:    [{ to: 'CLOSED', label: 'Close' }, { to: 'IN_PROGRESS', label: 'Reopen' }],
  CLOSED:      [],
};
const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function TicketDetailPage() {
  const id = Number(useParams().id);
  const qc = useQueryClient();
  const { canManage, userId } = usePermissions();
  const [comment, setComment] = useState('');
  const [internal, setInternal] = useState(false);

  const ticket = useQuery({ queryKey: ['ticket', id], queryFn: () => ticketsApi.get(id), enabled: Number.isFinite(id) });

  const refresh = async () => {
    await qc.invalidateQueries({ queryKey: ['ticket', id] });
    await qc.invalidateQueries({ queryKey: ['tickets'] });
    await qc.invalidateQueries({ queryKey: ['dashboard'] });
  };
  const status = useMutation({ mutationFn: (to: TicketStatus) => ticketsApi.changeStatus(id, to), onSuccess: refresh });
  const update = useMutation({
    mutationFn: (body: { priority?: TicketPriority; assignedToUserId?: number }) => ticketsApi.update(id, body),
    onSuccess: refresh,
  });
  const addComment = useMutation({
    mutationFn: () => ticketsApi.addComment(id, { body: comment.trim(), internal: canManage && internal }),
    onSuccess: async () => { setComment(''); setInternal(false); await refresh(); },
  });

  if (ticket.isLoading) return <><PageHeader title="Ticket" /><Card><div className="space-y-3 p-4"><Skeleton className="h-6 w-1/3" /><Skeleton className="h-4 w-2/3" /></div></Card></>;
  if (ticket.isError) return <><PageHeader title="Ticket" /><ErrorState error={ticket.error} /></>;
  if (!ticket.data) return null;
  const t = ticket.data;
  const assignee = t.assignedToUserId == null ? 'Unassigned' : t.assignedToUserId === userId ? 'You' : `User #${t.assignedToUserId}`;

  function submitComment(e: FormEvent) { e.preventDefault(); if (comment.trim()) addComment.mutate(); }

  return (
    <>
      <PageHeader
        title={`${t.ticketNumber} · ${t.subject}`}
        description={`Raised by ${t.reporterName} on ${fmtDateTime(t.createdAt)}`}
        actions={<Link to="/tickets" className="inline-flex items-center gap-1 text-sm text-slate-600 hover:text-slate-900"><ArrowLeft className="h-4 w-4" /> Back to tickets</Link>}
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Card>
            <CardHeader><div className="text-sm font-semibold text-slate-900">Description</div></CardHeader>
            <CardBody><p className="whitespace-pre-wrap text-sm text-slate-800">{t.description}</p></CardBody>
          </Card>

          <Card>
            <CardHeader><div className="text-sm font-semibold text-slate-900">Conversation ({t.comments?.length ?? 0})</div></CardHeader>
            <CardBody>
              {t.comments && t.comments.length > 0 ? (
                <ul className="space-y-3">
                  {t.comments.map(c => (
                    <li key={c.id} className={c.internal ? 'rounded-md border border-amber-200 bg-amber-50 p-3' : 'rounded-md border border-slate-200 p-3'}>
                      <div className="flex items-center gap-2 text-xs text-slate-500">
                        <span className="font-medium text-slate-700">{c.authorUserId === userId ? 'You' : `User #${c.authorUserId}`}</span>
                        <span>{fmtDateTime(c.createdAt)}</span>
                        {c.internal && <Badge tone="amber">Internal</Badge>}
                      </div>
                      <p className="mt-1 whitespace-pre-wrap text-sm text-slate-800">{c.body}</p>
                    </li>
                  ))}
                </ul>
              ) : <p className="text-sm text-slate-500">No comments yet.</p>}

              {t.status !== 'CLOSED' && (
                <form onSubmit={submitComment} className="mt-4 space-y-2">
                  <label htmlFor="new-comment" className="block text-xs font-medium text-slate-700">Add a comment</label>
                  <Textarea id="new-comment" rows={3} value={comment} onChange={e => setComment(e.target.value)} />
                  <div className="flex items-center justify-between">
                    {canManage ? (
                      <label htmlFor="comment-internal" className="flex items-center gap-2 text-xs text-slate-600">
                        <input id="comment-internal" type="checkbox" checked={internal} onChange={e => setInternal(e.target.checked)} />
                        Internal note (hidden from the reporter)
                      </label>
                    ) : <span />}
                    <Button type="submit" size="sm" loading={addComment.isPending} disabled={comment.trim() === ''}>Post comment</Button>
                  </div>
                  {addComment.isError && <ErrorState error={addComment.error} />}
                </form>
              )}
            </CardBody>
          </Card>
        </div>

        <Card>
          <CardHeader><div className="text-sm font-semibold text-slate-900">Details</div></CardHeader>
          <CardBody>
            <dl className="space-y-3 text-sm">
              <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Status</dt><dd className="mt-0.5"><TicketStatusBadge status={t.status} /></dd></div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Priority</dt>
                <dd className="mt-0.5">
                  {canManage && t.status !== 'CLOSED' ? (
                    <Select aria-label="Priority" value={t.priority} disabled={update.isPending}
                      onChange={e => update.mutate({ priority: e.target.value as TicketPriority })}>
                      {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
                    </Select>
                  ) : <TicketPriorityBadge priority={t.priority} />}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Assigned to</dt>
                <dd className="mt-0.5 flex items-center gap-2">
                  {assignee}
                  {canManage && userId != null && t.assignedToUserId !== userId && t.status !== 'CLOSED' && (
                    <Button size="sm" variant="secondary" loading={update.isPending}
                      onClick={() => update.mutate({ assignedToUserId: userId })}>Assign to me</Button>
                  )}
                </dd>
              </div>
              {t.relatedAssetId && (
                <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Related asset</dt>
                  <dd className="mt-0.5"><Link to={`/assets/${t.relatedAssetId}`} className="font-mono text-xs text-brand-700 hover:underline">{t.relatedAssetTag}</Link></dd></div>
              )}
              {t.resolvedAt && <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Resolved</dt><dd className="mt-0.5">{fmtDateTime(t.resolvedAt)}</dd></div>}
              {t.closedAt && <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Closed</dt><dd className="mt-0.5">{fmtDateTime(t.closedAt)}</dd></div>}
            </dl>

            {canManage && TRANSITIONS[t.status].length > 0 && (
              <div className="mt-5 border-t border-slate-200 pt-4">
                <div className="text-xs font-medium uppercase tracking-wide text-slate-500">Move ticket</div>
                <div className="mt-2 flex flex-wrap gap-2">
                  {TRANSITIONS[t.status].map(tr => (
                    <Button key={tr.to} size="sm" variant={tr.to === 'CLOSED' ? 'secondary' : 'primary'}
                      loading={status.isPending && status.variables === tr.to} disabled={status.isPending}
                      onClick={() => status.mutate(tr.to)}>{tr.label}</Button>
                  ))}
                </div>
              </div>
            )}
            {(status.isError || update.isError) && <ErrorState className="mt-3" error={status.error ?? update.error} />}
          </CardBody>
        </Card>
      </div>
    </>
  );
}
