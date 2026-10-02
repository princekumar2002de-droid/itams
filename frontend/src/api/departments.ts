import { api, type PageResponse } from '../lib/api';
import type { Department } from './types';

export const departmentsApi = {
  list(params: { q?: string; page?: number; size?: number } = {}) {
    return api<PageResponse<Department>>('/api/v1/departments', { query: params });
  },
  get(id: number) {
    return api<Department>(`/api/v1/departments/${id}`);
  },
  create(body: { code: string; name: string; parentDepartmentId?: number; managerPersonId?: number }) {
    return api<Department>('/api/v1/departments', { method: 'POST', body });
  },
  update(id: number, body: { name?: string; parentDepartmentId?: number | null; managerPersonId?: number | null }) {
    return api<Department>(`/api/v1/departments/${id}`, { method: 'PATCH', body });
  },
  remove(id: number) {
    return api<void>(`/api/v1/departments/${id}`, { method: 'DELETE' });
  },
};
