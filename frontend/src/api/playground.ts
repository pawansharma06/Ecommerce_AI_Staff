import { apiClient } from './client';

export interface ToolTraceItem {
  toolCallId: string;
  toolName: string;
  arguments: Record<string, any>;
  resultOutput: string;
  requiresApproval: boolean;
  status: string;
  executionMs: number;
}

export interface TraceStepItem {
  stepIndex: number;
  assistantThought: string;
  toolCalls: ToolTraceItem[];
  stepLatencyMs: number;
  stepTokens: number;
}

export interface InjectedMemoryItem {
  category: string;
  key: string;
  value: string;
}

export interface InjectedRagChunk {
  documentTitle: string;
  contentSnippet: string;
  similarityScore: number;
}

export interface InjectedContextSummary {
  systemPrompt: string;
  customerMemories: InjectedMemoryItem[];
  ragChunks: InjectedRagChunk[];
  graphRelationships: Record<string, any>[];
}

export interface PlaygroundTraceRequest {
  agentType: string;
  channel?: string;
  customerEmail?: string;
  userQuery: string;
  temperature?: number;
  maxSteps?: number;
}

export interface PlaygroundTraceResponse {
  agentType: string;
  channel: string;
  userQuery: string;
  injectedContext: InjectedContextSummary;
  traceSteps: TraceStepItem[];
  finalResponse: string;
  totalTokens: number;
  totalToolsExecuted: number;
  totalLatencyMs: number;
}

export interface PromptPreviewRequest {
  agentType: string;
  customerEmail?: string;
  userQuery: string;
}

export interface PromptPreviewResponse {
  agentType: string;
  customerEmail?: string;
  renderedPrompt: string;
  estimatedTokens: number;
  injectedMemories: InjectedMemoryItem[];
  injectedRagChunks: InjectedRagChunk[];
}

export const playgroundApi = {
  executeTrace: async (req: PlaygroundTraceRequest): Promise<PlaygroundTraceResponse> => {
    const res = await apiClient.post<{ success: boolean; data: PlaygroundTraceResponse }>('/playground/trace', req);
    return res.data.data;
  },

  previewPrompt: async (req: PromptPreviewRequest): Promise<PromptPreviewResponse> => {
    const res = await apiClient.post<{ success: boolean; data: PromptPreviewResponse }>('/playground/preview-prompt', req);
    return res.data.data;
  },
};
