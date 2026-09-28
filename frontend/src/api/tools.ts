import { apiClient } from './client';

export interface ToolDefinitionDto {
  name: string;
  description: string;
  requiredPermission: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  requiresHumanApproval: boolean;
  parametersSchema: Record<string, any>;
}

export interface ActionApprovalDto {
  id: string;
  agentName: string;
  toolName: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'PENDING' | 'APPROVED' | 'EXECUTED' | 'REJECTED' | 'FAILED';
  inputPayload: Record<string, any>;
  executionResult?: Record<string, any>;
  rejectionReason?: string;
  requestedBy: string;
  approvedBy?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface ToolExecutionResponse {
  status: 'SUCCESS' | 'PENDING_APPROVAL' | 'ERROR';
  toolName: string;
  result?: any;
  error?: string;
  actionRequestId?: string;
  riskLevel?: string;
  message?: string;
}

export const toolsApi = {
  getTools: async (): Promise<ToolDefinitionDto[]> => {
    const res = await apiClient.get('/tools');
    return res.data.data;
  },

  executeTool: async (
    toolName: string,
    parameters: Record<string, any>,
    agentName?: string
  ): Promise<ToolExecutionResponse> => {
    const res = await apiClient.post('/tools/execute', {
      toolName,
      parameters,
      agentName: agentName || 'ToolExplorer',
    });
    return res.data.data;
  },

  getApprovals: async (
    status?: string,
    page = 0,
    size = 20
  ): Promise<{ content: ActionApprovalDto[]; totalElements: number; totalPages: number }> => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await apiClient.get(`/approvals?${params.toString()}`);
    return res.data.data;
  },

  getPendingCount: async (): Promise<number> => {
    const res = await apiClient.get('/approvals/pending-count');
    return res.data.data.pendingCount;
  },

  approveAction: async (id: string): Promise<ActionApprovalDto> => {
    const res = await apiClient.post(`/approvals/${id}/approve`);
    return res.data.data;
  },

  rejectAction: async (id: string, reason?: string): Promise<ActionApprovalDto> => {
    const res = await apiClient.post(`/approvals/${id}/reject`, { reason });
    return res.data.data;
  },
};
