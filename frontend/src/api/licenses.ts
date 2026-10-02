import { api, type PageResponse } from '../lib/api';
import type { License, LicenseAssignment, LicenseType } from './types';

export const licensesApi = {
  list(params: { q?: string; page?: number; size?: number } = {}) {
    return api<PageResponse<License>>('/api/v1/licenses', { query: params });
  },
  get(id: number) {
    return api<License>(`/api/v1/licenses/${id}`);
  },
  assignments(id: number) {
    return api<LicenseAssignment[]>(`/api/v1/licenses/${id}/assignments`);
  },
  assign(id: number, body: { personId?: number; assetId?: number; notes?: string }) {
    return api<LicenseAssignment>(`/api/v1/licenses/${id}/assignments`, { method: 'POST', body });
  },
  create(body: CreateLicenseBody) {
    return api<License>('/api/v1/licenses', { method: 'POST', body });
  },
  release(assignmentId: number) {
    return api<LicenseAssignment>(`/api/v1/licenses/assignments/${assignmentId}/release`, { method: 'POST' });
  },
};

export interface CreateLicenseBody {
  vendor: string;
  productName: string;
  productVersion?: string;
  licenseReference: string;
  licenseType: LicenseType;
  seatsTotal: number;
  purchaseDate: string;
  expiresOn?: string;
  cost: number;
  procurementRef?: string;
}
