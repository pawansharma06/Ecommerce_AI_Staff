import { apiClient } from './client';

export type ChannelType = 'WEB_CHAT' | 'STOREFRONT_WIDGET' | 'WHATSAPP' | 'EMAIL';
export type MessageDirection = 'INBOUND' | 'OUTBOUND';
export type MessageStatus = 'RECEIVED' | 'PROCESSING' | 'SENT' | 'FAILED' | 'PENDING_APPROVAL';

export interface ChannelConfigDto {
  id?: string;
  channelType: ChannelType;
  enabled: boolean;
  autoReplyEnabled: boolean;
  configData: Record<string, any>;
  updatedAt: string;
}

export interface ChannelMessageDto {
  id: string;
  channelType: ChannelType;
  direction: MessageDirection;
  externalMessageId?: string;
  conversationId?: string;
  senderId: string;
  senderName?: string;
  recipientId: string;
  subject?: string;
  content: string;
  status: MessageStatus;
  errorMessage?: string;
  createdAt: string;
}

export interface ChannelSimulateRequest {
  channelType: ChannelType;
  senderId: string;
  senderName?: string;
  message: string;
  subject?: string;
}

export interface ChannelSimulateResponse {
  messageId: string;
  conversationId: string;
  channelType: ChannelType;
  inboundContent: string;
  outboundReply: string;
  toolsExecuted: number;
  latencyMs: number;
}

export const channelsApi = {
  getConfigs: async (): Promise<ChannelConfigDto[]> => {
    const res = await apiClient.get('/channels/configs');
    return res.data.data;
  },

  updateConfig: async (
    type: ChannelType,
    params: { enabled: boolean; autoReplyEnabled: boolean; configData: Record<string, any> }
  ): Promise<ChannelConfigDto> => {
    const res = await apiClient.put(`/channels/${type}/config`, params);
    return res.data.data;
  },

  simulateMessage: async (params: ChannelSimulateRequest): Promise<ChannelSimulateResponse> => {
    const res = await apiClient.post('/channels/simulate', params);
    return res.data.data;
  },

  getMessages: async (params?: {
    channelType?: ChannelType;
    search?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: ChannelMessageDto[]; totalElements: number; totalPages: number }> => {
    const query = new URLSearchParams();
    if (params?.channelType) query.append('channelType', params.channelType);
    if (params?.search) query.append('search', params.search);
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size !== undefined) query.append('size', params.size.toString());
    const res = await apiClient.get(`/channels/messages?${query.toString()}`);
    return res.data.data;
  },
};
