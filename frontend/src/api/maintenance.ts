import { api, type PageResponse } from '../lib/api';
import type { MaintenanceRecord } from './types';

export const maintenanceApi = {
  list(params: { q?: string; assetId?: number; page?: number; size?: number } = {}) {
    return api<PageResponse<MaintenanceRecord>>('/api/v1/maintenance', { query: params });
  },
  get(id: number) {
    return api<MaintenanceRecord>(`/api/v1/maintenance/${id}`);
  },
  create(body: CreateMaintenanceBody) {
    return api<MaintenanceRecord>('/api/v1/maintenance', { method: 'POST', body });
  },
  remove(id: number) {
    return api<void>(`/api/v1/maintenance/${id}`, { method: 'DELETE' });
  },
};

/** Exactly one performer: an internal user (performedByUserId) OR an external provider. */
export interface CreateMaintenanceBody {
  assetId: number;
  performedOn: string;
  performedByUserId?: number;
  providerName?: string;
  description: string;
  cost?: number;
  nextScheduledOn?: string;
}
