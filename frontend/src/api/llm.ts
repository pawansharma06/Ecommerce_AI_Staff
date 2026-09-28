import { apiClient } from './client';

export interface LlmStatusDto {
  activeProvider: string;
  activeModel: string;
  openaiConfigured: boolean;
  geminiConfigured: boolean;
  anthropicConfigured: boolean;
  totalTokensUsed: number;
  totalCostUsd: number;
}

export interface LlmUsageLogDto {
  id: string;
  provider: string;
  model: string;
  agentName: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  latencyMs: number;
  costUsd: number;
  createdAt: string;
}

export interface LlmGenerateResponse {
  content: string;
  providerName: string;
  modelName: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  finishReason: string;
}

export const llmApi = {
  getStatus: async (): Promise<LlmStatusDto> => {
    const res = await apiClient.get('/llm/status');
    return res.data.data;
  },

  getLogs: async (): Promise<LlmUsageLogDto[]> => {
    const res = await apiClient.get('/llm/logs');
    return res.data.data;
  },

  generate: async (
    userPrompt: string,
    systemPrompt?: string,
    agentName?: string,
    temperature?: number
  ): Promise<LlmGenerateResponse> => {
    const res = await apiClient.post('/llm/generate', {
      userPrompt,
      systemPrompt,
      agentName,
      temperature,
    });
    return res.data.data;
  },
};
