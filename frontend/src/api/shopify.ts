import { apiClient } from './client';
import { ApiResponse } from './auth';

export interface ShopifyConfig {
  id?: string;
  shopDomain: string;
  isAccessTokenConfigured: boolean;
  isWebhookSecretConfigured: boolean;
  apiVersion: string;
  shopName?: string;
  shopOwner?: string;
  email?: string;
  currency?: string;
  timezone?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ShopifyConfigRequest {
  shopDomain: string;
  adminAccessToken: string;
  webhookSecret?: string;
  apiVersion?: string;
}

export interface ShopifyTestResponse {
  success: boolean;
  message: string;
  shopDetails?: Record<string, any>;
}

export interface WebhookEvent {
  id: string;
  topic: string;
  shopifyWebhookId?: string;
  shopDomain: string;
  apiVersion: string;
  status: string;
  attempts: number;
  errorMessage?: string;
  processedAt?: string;
  createdAt: string;
}

export const shopifyApi = {
  async getConfig(): Promise<ShopifyConfig> {
    const res = await apiClient.get<ApiResponse<ShopifyConfig>>('/shopify/config');
    return res.data.data;
  },

  async updateConfig(data: ShopifyConfigRequest): Promise<ShopifyConfig> {
    const res = await apiClient.post<ApiResponse<ShopifyConfig>>('/shopify/config', data);
    return res.data.data;
  },

  async testConnection(): Promise<ShopifyTestResponse> {
    const res = await apiClient.post<ApiResponse<ShopifyTestResponse>>('/shopify/config/test');
    return res.data.data;
  },

  async getRecentWebhooks(): Promise<WebhookEvent[]> {
    const res = await apiClient.get<ApiResponse<WebhookEvent[]>>('/shopify/config/webhooks');
    return res.data.data;
  },
};
