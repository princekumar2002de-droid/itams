import { Badge } from './Badge';
import type { TicketPriority, TicketStatus } from '../../api/types';

export function TicketPriorityBadge({ priority }: { priority: TicketPriority }) {
  const tone = priority === 'CRITICAL' ? 'red'
             : priority === 'HIGH'     ? 'amber'
             : priority === 'MEDIUM'   ? 'blue'
             : 'gray';
  return <Badge tone={tone}>{priority}</Badge>;
}

export function TicketStatusBadge({ status }: { status: TicketStatus }) {
  const tone = status === 'OPEN'         ? 'blue'
             : status === 'IN_PROGRESS'  ? 'amber'
             : status === 'WAITING'      ? 'slate'
             : status === 'RESOLVED'     ? 'green'
             : 'gray';
  return <Badge tone={tone}>{status.replaceAll('_', ' ')}</Badge>;
}
