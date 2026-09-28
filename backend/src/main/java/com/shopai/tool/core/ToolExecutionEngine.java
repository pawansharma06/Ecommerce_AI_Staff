package com.shopai.tool.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.approval.domain.ActionRequest;
import com.shopai.approval.service.ActionApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ToolExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionEngine.class);

    private final ToolRegistry toolRegistry;
    private final ActionApprovalService actionApprovalService;
    private final ObjectMapper objectMapper;

    public ToolExecutionEngine(
            ToolRegistry toolRegistry,
            ActionApprovalService actionApprovalService,
            ObjectMapper objectMapper
    ) {
        this.toolRegistry = toolRegistry;
        this.actionApprovalService = actionApprovalService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> executeTool(String toolName, Map<String, Object> params, String agentName, String requestedBy) {
        Optional<Tool> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        Tool tool = toolOpt.get();
        log.info("ToolExecutionEngine requested execution of [{}] by agent [{}] (Risk: {})", toolName, agentName, tool.getRiskLevel());

        // Human Approval Gate for HIGH / CRITICAL actions (Rule 14 & Rule 38)
        if (tool.requiresHumanApproval()) {
            ActionRequest req = actionApprovalService.createActionRequest(
                    agentName,
                    tool.getName(),
                    tool.getRiskLevel(),
                    params,
                    requestedBy
            );

            Map<String, Object> pendingResp = new HashMap<>();
            pendingResp.put("status", "PENDING_APPROVAL");
            pendingResp.put("actionRequestId", req.getId());
            pendingResp.put("toolName", tool.getName());
            pendingResp.put("riskLevel", tool.getRiskLevel().name());
            pendingResp.put("message", "This action has high operational risk and has been submitted to the merchant approval queue.");
            return pendingResp;
        }

        // Direct Execution for LOW and MEDIUM Risk Actions
        try {
            Object result = tool.execute(params != null ? params : Map.of());
            Map<String, Object> successResp = new HashMap<>();
            successResp.put("status", "SUCCESS");
            successResp.put("toolName", tool.getName());
            successResp.put("result", result);
            return successResp;
        } catch (Exception e) {
            log.error("Execution error in tool [{}]: {}", toolName, e.getMessage(), e);
            Map<String, Object> errResp = new HashMap<>();
            errResp.put("status", "ERROR");
            errResp.put("toolName", tool.getName());
            errResp.put("error", e.getMessage());
            return errResp;
        }
    }
}