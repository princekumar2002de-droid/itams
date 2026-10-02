import { api, type PageResponse } from '../lib/api';
import type { Person } from './types';

export const peopleApi = {
  list(params: { q?: string; page?: number; size?: number } = {}) {
    return api<PageResponse<Person>>('/api/v1/people', { query: params });
  },
  create(body: { firstName: string; lastName: string; email: string; phone?: string }) {
    return api<Person>('/api/v1/people', { method: 'POST', body });
  },
  update(id: number, body: { firstName?: string; lastName?: string; email?: string; phone?: string; active?: boolean }) {
    return api<Person>(`/api/v1/people/${id}`, { method: 'PATCH', body });
  },
};
