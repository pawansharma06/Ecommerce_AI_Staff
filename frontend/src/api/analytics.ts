import { apiClient } from './client';

export interface AnalyticsOverviewDto {
  totalGmv: number;
  totalOrders: number;
  averageOrderValue: number;
  totalCustomers: number;
  activeProducts: number;
  unfulfilledOrders: number;
  abandonedCheckouts: number;
  totalAiConversations: number;
  totalAiMessages: number;
  totalTokensConsumed: number;
  estimatedLlmCostUsd: number;
  pendingApprovalRate: number;
}

export interface SalesTrendPointDto {
  date: string;
  revenue: number;
  orderCount: number;
  averageOrderValue: number;
}

export interface AiMetricsDto {
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  estimatedCostUsd: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
  totalConversations: number;
  totalMessages: number;
  toolInvocations: Record<string, number>;
  channelDistribution: Record<string, number>;
  approvalStats: Record<string, number>;
}

export interface GenerateReportRequest {
  timeframe?: string;
  focusArea?: string;
  customPrompt?: string;
}

export interface ExecutiveReportResponse {
  title: string;
  generatedAt: string;
  timeframe: string;
  executiveSummary: string;
  keyHighlights: string[];
  operationalAlerts: string[];
  strategicRecommendations: string[];
  metricsSnapshot: Record<string, any>;
}

export const analyticsApi = {
  getOverview: async (): Promise<AnalyticsOverviewDto> => {
    const res = await apiClient.get<{ success: boolean; data: AnalyticsOverviewDto }>('/analytics/overview');
    return res.data.data;
  },

  getSalesTrends: async (days = 14): Promise<SalesTrendPointDto[]> => {
    const res = await apiClient.get<{ success: boolean; data: SalesTrendPointDto[] }>(`/analytics/sales?days=${days}`);
    return res.data.data;
  },

  getAiMetrics: async (): Promise<AiMetricsDto> => {
    const res = await apiClient.get<{ success: boolean; data: AiMetricsDto }>('/analytics/ai-metrics');
    return res.data.data;
  },

  generateReport: async (req: GenerateReportRequest = {}): Promise<ExecutiveReportResponse> => {
    const res = await apiClient.post<{ success: boolean; data: ExecutiveReportResponse }>('/analytics/report/generate', req);
    return res.data.data;
  },
};
