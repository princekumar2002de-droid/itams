import { api } from '../lib/api';
import type { DashboardStats } from './types';

export const statsApi = {
  dashboard() { return api<DashboardStats>('/api/v1/stats/dashboard'); },
};
