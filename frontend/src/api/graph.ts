import { apiClient } from './client';

export interface GraphNode {
  id: string;
  label: string;
  type: string;
  metadata?: Record<string, any>;
}

export interface GraphEdge {
  id: string;
  source: string;
  sourceType: string;
  target: string;
  targetType: string;
  relationshipType: string;
  weight: number;
  confidence: number;
  metadata?: Record<string, any>;
}

export interface GraphSubgraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface GraphStats {
  totalEdges: number;
  totalNodes: number;
  relationshipCounts: Record<string, number>;
}

export interface RebuildGraphResult {
  edgesCreated: number;
  durationMs: number;
  countsByType: Record<string, number>;
  message: string;
}

export const graphApi = {
  getStats: async (): Promise<GraphStats> => {
    const res = await apiClient.get('/graph/stats');
    return res.data.data;
  },

  explore: async (params?: {
    entityType?: string;
    entityId?: string;
    depth?: number;
    limit?: number;
  }): Promise<GraphSubgraph> => {
    const query = new URLSearchParams();
    if (params?.entityType) query.append('entityType', params.entityType);
    if (params?.entityId) query.append('entityId', params.entityId);
    if (params?.depth) query.append('depth', params.depth.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    const res = await apiClient.get(`/graph/explore?${query.toString()}`);
    return res.data.data;
  },

  getFrequentlyBoughtTogether: async (productId: string, limit = 5): Promise<any[]> => {
    const res = await apiClient.get(`/graph/frequently-bought-together?productId=${productId}&limit=${limit}`);
    return res.data.data;
  },

  getRelatedProducts: async (productId: string, limit = 5): Promise<any[]> => {
    const res = await apiClient.get(`/graph/related-products?productId=${productId}&limit=${limit}`);
    return res.data.data;
  },

  getCustomerPurchases: async (customerEmail: string): Promise<any[]> => {
    const res = await apiClient.get(`/graph/customer-purchases?customerEmail=${encodeURIComponent(customerEmail)}`);
    return res.data.data;
  },

  rebuildGraph: async (): Promise<RebuildGraphResult> => {
    const res = await apiClient.post('/graph/rebuild');
    return res.data.data;
  },
};
