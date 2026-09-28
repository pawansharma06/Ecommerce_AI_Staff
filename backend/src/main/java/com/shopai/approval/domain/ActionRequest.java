package com.shopai.approval.domain;

import com.shopai.tool.core.ToolRiskLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_requests")
public class ActionRequest {

    @Id
    private UUID id;

    @Column(name = "agent_name", nullable = false, length = 100)
    private String agentName;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private ToolRiskLevel riskLevel;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED, EXECUTED, FAILED

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "input_payload", nullable = false, columnDefinition = "jsonb")
    private String inputPayload;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "execution_result", columnDefinition = "jsonb")
    private String executionResult;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public ActionRequest() {
        this.id = UUID.randomUUID();
        this.status = "PENDING";
        this.inputPayload = "{}";
        this.createdAt = Instant.now();
    }

    public ActionRequest(String agentName, String toolName, ToolRiskLevel riskLevel, String inputPayload, String requestedBy) {
        this();
        this.agentName = agentName;
        this.toolName = toolName;
        this.riskLevel = riskLevel;
        this.inputPayload = inputPayload != null ? inputPayload : "{}";
        this.requestedBy = requestedBy;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public ToolRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ToolRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getInputPayload() { return inputPayload; }
    public void setInputPayload(String inputPayload) { this.inputPayload = inputPayload; }
    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}