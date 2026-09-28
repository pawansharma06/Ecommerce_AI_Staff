import { apiClient } from './client';
import { ApiResponse } from './auth';

export interface SyncJob {
  id: string;
  jobType: string;
  status: string;
  itemsProcessed: number;
  errorMessage?: string;
  startedAt: string;
  completedAt?: string;
}

export const syncApi = {
  async triggerCatalogSync(): Promise<{ message: string }> {
    const res = await apiClient.post<ApiResponse<{ message: string }>>('/sync/catalog');
    return res.data.data;
  },

  async triggerOrderSync(): Promise<{ message: string }> {
    const res = await apiClient.post<ApiResponse<{ message: string }>>('/sync/orders');
    return res.data.data;
  },

  async getRecentJobs(): Promise<SyncJob[]> {
    const res = await apiClient.get<ApiResponse<SyncJob[]>>('/sync/status');
    return res.data.data;
  },
};
