import { apiClient } from './client';

export interface MessageDto {
  id: string;
  conversationId: string;
  role: 'system' | 'user' | 'assistant' | 'tool';
  content: string;
  toolCalls?: any;
  toolCallId?: string;
  toolName?: string;
  tokenCount: number;
  createdAt: string;
}

export interface ConversationDto {
  id: string;
  title: string;
  agentType: 'CUSTOMER_SUPPORT' | 'ADMIN_COPILOT';
  channel: 'WEB_CHAT' | 'ADMIN_COPILOT' | 'STOREFRONT_WIDGET';
  customerEmail?: string;
  customerId?: number;
  metadata?: Record<string, any>;
  messages?: MessageDto[];
  createdAt: string;
  updatedAt: string;
}

export interface AgentTurnResultDto {
  conversationId: string;
  userMessage: MessageDto;
  assistantMessage: MessageDto;
  toolsExecuted: Array<{
    step: number;
    toolName: string;
    arguments: Record<string, any>;
    result: any;
  }>;
  pendingApprovals: Array<{
    status: string;
    actionRequestId?: string;
    toolName?: string;
    riskLevel?: string;
    message?: string;
  }>;
  totalTokensUsed: number;
  latencyMs: number;
}

export const agentApi = {
  createConversation: async (params: {
    title?: string;
    agentType: 'CUSTOMER_SUPPORT' | 'ADMIN_COPILOT';
    channel?: string;
    customerEmail?: string;
    customerId?: number;
    metadata?: Record<string, any>;
  }): Promise<ConversationDto> => {
    const res = await apiClient.post('/conversations', params);
    return res.data.data;
  },

  listConversations: async (
    agentType?: string,
    customerEmail?: string,
    search?: string,
    page = 0,
    size = 20
  ): Promise<{ content: ConversationDto[]; totalElements: number; totalPages: number }> => {
    const query = new URLSearchParams();
    if (agentType && agentType !== 'ALL') query.append('agentType', agentType);
    if (customerEmail) query.append('customerEmail', customerEmail);
    if (search) query.append('search', search);
    query.append('page', page.toString());
    query.append('size', size.toString());
    const res = await apiClient.get(`/conversations?${query.toString()}`);
    return res.data.data;
  },

  getConversation: async (id: string): Promise<ConversationDto> => {
    const res = await apiClient.get(`/conversations/${id}`);
    return res.data.data;
  },

  sendMessage: async (
    id: string,
    content: string,
    customerEmail?: string
  ): Promise<AgentTurnResultDto> => {
    const res = await apiClient.post(`/conversations/${id}/messages`, {
      content,
      customerEmail,
    });
    return res.data.data;
  },

  deleteConversation: async (id: string): Promise<void> => {
    await apiClient.delete(`/conversations/${id}`);
  },
};
