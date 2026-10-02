import { api, type PageResponse } from '../lib/api';
import type { AssetAssignment, AssetCondition } from './types';

export interface AssignBody {
  assetId: number;
  assigneePersonId: number;
  expectedReturnOn?: string;
  outCondition: AssetCondition;
  notes?: string;
}

export interface ReturnBody {
  inCondition: AssetCondition;
  sendForMaintenance: boolean;
  notes?: string;
}

export const assignmentsApi = {
  list(params: { assetId?: number; personId?: number; onlyOpen?: boolean; page?: number; size?: number } = {}) {
    return api<PageResponse<AssetAssignment>>('/api/v1/assignments', { query: params });
  },
  get(id: number) {
    return api<AssetAssignment>(`/api/v1/assignments/${id}`);
  },
  assign(body: AssignBody) {
    return api<AssetAssignment>('/api/v1/assignments', { method: 'POST', body });
  },
  returnIt(id: number, body: ReturnBody) {
    return api<AssetAssignment>(`/api/v1/assignments/${id}/return`, { method: 'POST', body });
  },
};
