package com.shopai.approval.dto;

import com.shopai.approval.domain.ActionRequest;
import com.shopai.tool.core.ToolRiskLevel;

import java.time.Instant;
import java.util.UUID;

public record ActionApprovalResponse(
        UUID id,
        String agentName,
        String toolName,
        ToolRiskLevel riskLevel,
        String status,
        String inputPayload,
        String executionResult,
        String rejectionReason,
        String requestedBy,
        String approvedBy,
        Instant createdAt,
        Instant resolvedAt
) {
    public static ActionApprovalResponse from(ActionRequest a) {
        return new ActionApprovalResponse(
                a.getId(),
                a.getAgentName(),
                a.getToolName(),
                a.getRiskLevel(),
                a.getStatus(),
                a.getInputPayload(),
                a.getExecutionResult(),
                a.getRejectionReason(),
                a.getRequestedBy(),
                a.getApprovedBy(),
                a.getCreatedAt(),
                a.getResolvedAt()
        );
    }
}