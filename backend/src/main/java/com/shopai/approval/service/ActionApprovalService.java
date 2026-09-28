package com.shopai.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.approval.domain.ActionRequest;
import com.shopai.approval.dto.ActionApprovalResponse;
import com.shopai.approval.repository.ActionRequestRepository;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRegistry;
import com.shopai.tool.core.ToolRiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActionApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ActionApprovalService.class);

    private final ActionRequestRepository actionRequestRepository;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ActionApprovalService(
            ActionRequestRepository actionRequestRepository,
            ToolRegistry toolRegistry,
            ObjectMapper objectMapper
    ) {
        this.actionRequestRepository = actionRequestRepository;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ActionRequest createActionRequest(String agentName, String toolName, ToolRiskLevel riskLevel, Map<String, Object> params, String requestedBy) {
        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(params != null ? params : Map.of());
        } catch (Exception e) {
            log.warn("Failed to serialize action request payload: {}", e.getMessage());
        }

        ActionRequest req = new ActionRequest(
                agentName != null ? agentName : "AGENT",
                toolName,
                riskLevel,
                payloadJson,
                requestedBy
        );
        req = actionRequestRepository.save(req);
        log.warn("Created PENDING ActionRequest {} for HIGH/CRITICAL risk tool [{}]", req.getId(), toolName);
        return req;
    }

    @Transactional(readOnly = true)
    public Page<ActionApprovalResponse> getActionRequests(String status, int page, int size) {
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.trim().toUpperCase() : null;
        return actionRequestRepository.findWithFilters(cleanStatus, PageRequest.of(page, size))
                .map(ActionApprovalResponse::from);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return actionRequestRepository.countByStatus("PENDING");
    }

    @Transactional
    public ActionApprovalResponse approveAndExecute(UUID id, String approvedBy) {
        ActionRequest req = actionRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ActionRequest not found: " + id));

        if (!"PENDING".equals(req.getStatus())) {
            throw new IllegalStateException("ActionRequest is already resolved: " + req.getStatus());
        }

        req.setApprovedBy(approvedBy);
        req.setStatus("APPROVED");
        req.setResolvedAt(Instant.now());

        // Execute tool
        try {
            Optional<Tool> toolOpt = toolRegistry.getTool(req.getToolName());
            if (toolOpt.isEmpty()) {
                throw new IllegalStateException("Registered tool not found: " + req.getToolName());
            }
            Tool tool = toolOpt.get();
            Map<String, Object> params = objectMapper.readValue(req.getInputPayload(), Map.class);
            Object result = tool.execute(params);
            req.setExecutionResult(objectMapper.writeValueAsString(result));
            req.setStatus("EXECUTED");
            log.info("ActionRequest {} APPROVED and EXECUTED by {}. Result: {}", id, approvedBy, req.getExecutionResult());
        } catch (Exception e) {
            log.error("Failed to execute approved ActionRequest {}: {}", id, e.getMessage(), e);
            req.setStatus("FAILED");
            req.setExecutionResult("{\"error\":\"" + e.getMessage() + "\"}");
        }

        req = actionRequestRepository.save(req);
        return ActionApprovalResponse.from(req);
    }

    @Transactional
    public ActionApprovalResponse reject(UUID id, String rejectionReason, String rejectedBy) {
        ActionRequest req = actionRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ActionRequest not found: " + id));

        if (!"PENDING".equals(req.getStatus())) {
            throw new IllegalStateException("ActionRequest is already resolved: " + req.getStatus());
        }

        req.setStatus("REJECTED");
        req.setRejectionReason(rejectionReason != null ? rejectionReason : "Rejected by merchant operator");
        req.setApprovedBy(rejectedBy);
        req.setResolvedAt(Instant.now());
        req = actionRequestRepository.save(req);
        log.info("ActionRequest {} REJECTED by {}: {}", id, rejectedBy, req.getRejectionReason());
        return ActionApprovalResponse.from(req);
    }
}