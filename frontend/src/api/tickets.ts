import { api, type PageResponse } from '../lib/api';
import type { Ticket, TicketComment, TicketPriority, TicketStatus } from './types';

export interface CreateTicketBody {
  subject: string;
  description: string;
  priority: TicketPriority;
  reporterPersonId: number;
  relatedAssetId?: number;
}

export const ticketsApi = {
  list(params: {
    q?: string;
    status?: TicketStatus;
    priority?: TicketPriority;
    assignedToUserId?: number;
    reporterPersonId?: number;
    page?: number;
    size?: number;
  } = {}) {
    return api<PageResponse<Ticket>>('/api/v1/tickets', { query: params });
  },
  get(id: number) {
    return api<Ticket>(`/api/v1/tickets/${id}`);
  },
  create(body: CreateTicketBody) {
    return api<Ticket>('/api/v1/tickets', { method: 'POST', body });
  },
  update(id: number, body: { priority?: TicketPriority; assignedToUserId?: number }) {
    return api<Ticket>(`/api/v1/tickets/${id}`, { method: 'PATCH', body });
  },
  changeStatus(id: number, to: TicketStatus) {
    return api<Ticket>(`/api/v1/tickets/${id}/status`, { method: 'POST', body: { to } });
  },
  addComment(id: number, body: { body: string; internal: boolean }) {
    return api<TicketComment>(`/api/v1/tickets/${id}/comments`, { method: 'POST', body });
  },
};
