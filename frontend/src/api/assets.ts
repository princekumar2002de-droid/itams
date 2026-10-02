import { api, type PageResponse } from '../lib/api';
import type { Asset, AssetCategory, AssetModel, AssetStatus } from './types';

// `type`, not `interface`: only type aliases are assignable to api()'s Record<string, …> query param.
export type AssetsListParams = {
  q?: string;
  status?: AssetStatus;
  categoryId?: number;
  page?: number;
  size?: number;
};

export const assetsApi = {
  list(params: AssetsListParams = {}) {
    return api<PageResponse<Asset>>('/api/v1/assets', { query: params });
  },
  get(id: number) {
    return api<Asset>(`/api/v1/assets/${id}`);
  },
  categories() {
    return api<AssetCategory[]>('/api/v1/asset-categories');
  },
  models(params: { q?: string; categoryId?: number; page?: number; size?: number } = {}) {
    return api<PageResponse<AssetModel>>('/api/v1/asset-models', { query: params });
  },
  createModel(body: { categoryId: number; manufacturer: string; modelName: string; specs?: string }) {
    return api<AssetModel>('/api/v1/asset-models', { method: 'POST', body });
  },
  create(body: CreateAssetBody) {
    return api<Asset>('/api/v1/assets', { method: 'POST', body });
  },
  update(id: number, body: { warrantyEndsOn?: string | null; notes?: string | null }) {
    return api<Asset>(`/api/v1/assets/${id}`, { method: 'PATCH', body });
  },
  retire(id: number, reason: string) {
    return api<Asset>(`/api/v1/assets/${id}/retire`, { method: 'POST', body: { reason } });
  },
  completeMaintenance(id: number) {
    return api<Asset>(`/api/v1/assets/${id}/maintenance-complete`, { method: 'POST' });
  },
};

export interface CreateAssetBody {
  assetTag: string;
  modelId: number;
  serialNumber: string;
  purchaseDate: string;
  purchasePrice: number;
  warrantyEndsOn?: string;
  notes?: string;
}
