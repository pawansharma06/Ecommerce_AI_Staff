package com.shopai.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.approval.domain.ActionRequest;
import com.shopai.approval.dto.ActionApprovalResponse;
import com.shopai.approval.repository.ActionRequestRepository;
import com.shopai.approval.service.ActionApprovalService;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRegistry;
import com.shopai.tool.core.ToolRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActionApprovalServiceTest {

    private ActionRequestRepository actionRequestRepository;
    private ToolRegistry toolRegistry;
    private ObjectMapper objectMapper;
    private ActionApprovalService approvalService;

    @BeforeEach
    void setup() {
        actionRequestRepository = Mockito.mock(ActionRequestRepository.class);
        toolRegistry = Mockito.mock(ToolRegistry.class);
        objectMapper = new ObjectMapper();
        approvalService = new ActionApprovalService(actionRequestRepository, toolRegistry, objectMapper);
    }

    @Test
    @DisplayName("createActionRequest creates a pending request with serialized payload")
    void testCreateActionRequest() {
        when(actionRequestRepository.save(any(ActionRequest.class))).thenAnswer(i -> {
            ActionRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ActionRequest req = approvalService.createActionRequest(
                "RefundAgent", "refund_order", ToolRiskLevel.HIGH, Map.of("orderNumber", "1001", "amount", 50.0), "operator"
        );

        assertNotNull(req);
        assertEquals("PENDING", req.getStatus());
        assertEquals("refund_order", req.getToolName());
        assertEquals(ToolRiskLevel.HIGH, req.getRiskLevel());
        assertTrue(req.getInputPayload().contains("1001"));
        verify(actionRequestRepository, times(1)).save(any(ActionRequest.class));
    }

    @Test
    @DisplayName("approveAndExecute executes the registered tool and sets EXECUTED status")
    void testApproveAndExecute() throws Exception {
        UUID id = UUID.randomUUID();
        ActionRequest req = new ActionRequest("RefundAgent", "refund_order", ToolRiskLevel.HIGH, "{\"orderNumber\":\"1001\"}", "operator");
        req.setId(id);
        req.setStatus("PENDING");

        Tool mockTool = Mockito.mock(Tool.class);
        when(mockTool.execute(any())).thenReturn(Map.of("success", true, "refunded", 50.0));
        when(toolRegistry.getTool("refund_order")).thenReturn(Optional.of(mockTool));
        when(actionRequestRepository.findById(id)).thenReturn(Optional.of(req));
        when(actionRequestRepository.save(any(ActionRequest.class))).thenAnswer(i -> i.getArgument(0));

        ActionApprovalResponse response = approvalService.approveAndExecute(id, "admin_user");

        assertNotNull(response);
        assertEquals("EXECUTED", response.status());
        assertEquals("admin_user", response.approvedBy());
        assertNotNull(response.executionResult());
        verify(mockTool, times(1)).execute(any());
    }

    @Test
    @DisplayName("reject sets status to REJECTED with rejection reason")
    void testRejectAction() {
        UUID id = UUID.randomUUID();
        ActionRequest req = new ActionRequest("RefundAgent", "refund_order", ToolRiskLevel.HIGH, "{\"orderNumber\":\"1001\"}", "operator");
        req.setId(id);
        req.setStatus("PENDING");

        when(actionRequestRepository.findById(id)).thenReturn(Optional.of(req));
        when(actionRequestRepository.save(any(ActionRequest.class))).thenAnswer(i -> i.getArgument(0));

        ActionApprovalResponse response = approvalService.reject(id, "Customer issue already resolved", "admin_user");

        assertNotNull(response);
        assertEquals("REJECTED", response.status());
        assertEquals("Customer issue already resolved", response.rejectionReason());
        assertEquals("admin_user", response.approvedBy());
    }
}
