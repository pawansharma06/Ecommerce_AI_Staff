import { apiClient } from './client';

export interface RagStats {
  totalDocuments: number;
  totalChunks: number;
  totalCustomerMemories: number;
  activeEmbeddingProvider: string;
  activeEmbeddingModel: string;
  vectorDimensions: number;
  hnswIndexActive: boolean;
  documentCountsByType: Record<string, number>;
}

export interface SearchResultItem {
  id: string;
  documentId: string;
  documentTitle: string;
  documentType: string;
  content: string;
  tokenCount: number;
  customerEmail: string | null;
  similarityScore: number;
  metadata: Record<string, any>;
}

export interface SearchRequest {
  query: string;
  customerEmail?: string;
  documentType?: string;
  minScore?: number;
  limit?: number;
}

export interface KnowledgeDocument {
  id: string;
  title: string;
  content: string;
  documentType: string;
  sourceId?: string;
  customerId?: number;
  customerEmail?: string;
  status: string;
  chunkCount: number;
  createdAt: string;
  updatedAt: string;
  metadata?: Record<string, any>;
}

export interface CustomerMemory {
  id: string;
  customerId?: number;
  customerEmail: string;
  category: string;
  memoryKey: string;
  memoryValue: string;
  confidenceScore: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDocumentRequest {
  title: string;
  content: string;
  documentType: string;
  customerEmail?: string;
  metadata?: Record<string, any>;
}

export interface CreateCustomerMemoryRequest {
  customerEmail: string;
  category: string;
  key: string;
  value: string;
  confidenceScore?: number;
}

export const ragApi = {
  getStats: async (): Promise<RagStats> => {
    const res = await apiClient.get<RagStats>('/rag/stats');
    return res.data;
  },

  search: async (req: SearchRequest): Promise<SearchResultItem[]> => {
    const res = await apiClient.post<SearchResultItem[]>('/rag/search', req);
    return res.data;
  },

  triggerReindex: async (): Promise<{ message: string; timestamp: string }> => {
    const res = await apiClient.post<{ message: string; timestamp: string }>('/rag/index');
    return res.data;
  },

  getDocuments: async (params?: { page?: number; size?: number; type?: string; customerEmail?: string }): Promise<{
    content: KnowledgeDocument[];
    totalElements: number;
    totalPages: number;
    number: number;
  }> => {
    const res = await apiClient.get('/rag/documents', { params });
    return res.data;
  },

  createDocument: async (doc: CreateDocumentRequest): Promise<KnowledgeDocument> => {
    const res = await apiClient.post<KnowledgeDocument>('/rag/documents', doc);
    return res.data;
  },

  deleteDocument: async (id: string): Promise<void> => {
    await apiClient.delete(`/rag/documents/${id}`);
  },

  getCustomerMemories: async (email: string): Promise<CustomerMemory[]> => {
    const res = await apiClient.get<CustomerMemory[]>('/customers/memory', { params: { email } });
    return res.data;
  },

  createCustomerMemory: async (req: CreateCustomerMemoryRequest): Promise<CustomerMemory> => {
    const res = await apiClient.post<CustomerMemory>('/customers/memory', req);
    return res.data;
  },

  deleteCustomerMemory: async (id: string): Promise<void> => {
    await apiClient.delete(`/customers/memory/${id}`);
  },

  searchCustomerMemories: async (req: { email: string; query: string; minScore?: number; limit?: number }): Promise<any[]> => {
    const res = await apiClient.post<any[]>('/customers/memory/search', req);
    return res.data;
  },
};