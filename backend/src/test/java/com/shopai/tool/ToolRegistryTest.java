package com.shopai.tool;

import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolExecutionEngine;
import com.shopai.tool.core.ToolRegistry;
import com.shopai.tool.core.ToolRiskLevel;
import com.shopai.approval.service.ActionApprovalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ToolRegistryTest {

    static class DummyLowRiskTool implements Tool {
        @Override
        public String getName() { return "dummy_search"; }
        @Override
        public String getDescription() { return "Search dummy products"; }
        @Override
        public String getRequiredPermission() { return "product.read"; }
        @Override
        public ToolRiskLevel getRiskLevel() { return ToolRiskLevel.LOW; }
        @Override
        public Map<String, Object> getParametersSchema() { return Map.of("type", "object"); }
        @Override
        public Object execute(Map<String, Object> params) { return Map.of("status", "ok", "items", List.of("item1")); }
    }

    static class DummyHighRiskTool implements Tool {
        @Override
        public String getName() { return "dummy_refund"; }
        @Override
        public String getDescription() { return "Refund an order"; }
        @Override
        public String getRequiredPermission() { return "order.refund"; }
        @Override
        public ToolRiskLevel getRiskLevel() { return ToolRiskLevel.HIGH; }
        @Override
        public boolean requiresHumanApproval() { return true; }
        @Override
        public Map<String, Object> getParametersSchema() { return Map.of("type", "object"); }
        @Override
        public Object execute(Map<String, Object> params) { return Map.of("refunded", true); }
    }

    @Test
    @DisplayName("ToolRegistry registers tools and looks them up by name")
    void testToolRegistration() {
        Tool lowTool = new DummyLowRiskTool();
        Tool highTool = new DummyHighRiskTool();
        ToolRegistry registry = new ToolRegistry(List.of(lowTool, highTool));

        assertEquals(2, registry.getAllTools().size());
        Optional<Tool> found = registry.getTool("dummy_search");
        assertTrue(found.isPresent());
        assertEquals("dummy_search", found.get().getName());
        assertEquals(ToolRiskLevel.LOW, found.get().getRiskLevel());

        assertTrue(registry.getTool("non_existent").isEmpty());
    }

    @Test
    @DisplayName("ToolExecutionEngine executes low-risk tool immediately")
    void testExecuteLowRiskTool() {
        Tool lowTool = new DummyLowRiskTool();
        ToolRegistry registry = new ToolRegistry(List.of(lowTool));
        ActionApprovalService approvalService = Mockito.mock(ActionApprovalService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ToolExecutionEngine engine = new ToolExecutionEngine(registry, approvalService, objectMapper);

        Map<String, Object> result = engine.executeTool("dummy_search", Map.of("q", "shoes"), "SearchAgent", "operator");

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("dummy_search", result.get("toolName"));
        verifyNoInteractions(approvalService);
    }

    @Test
    @DisplayName("ToolExecutionEngine routes high-risk tool to human approval queue")
    void testExecuteHighRiskToolRoutesToApproval() {
        Tool highTool = new DummyHighRiskTool();
        ToolRegistry registry = new ToolRegistry(List.of(highTool));
        ActionApprovalService approvalService = Mockito.mock(ActionApprovalService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        com.shopai.approval.domain.ActionRequest mockReq = new com.shopai.approval.domain.ActionRequest(
                "RefundAgent", "dummy_refund", ToolRiskLevel.HIGH, "{}", "operator"
        );
        mockReq.setId(java.util.UUID.randomUUID());

        when(approvalService.createActionRequest(eq("RefundAgent"), eq("dummy_refund"), eq(ToolRiskLevel.HIGH), any(), eq("operator")))
                .thenReturn(mockReq);

        ToolExecutionEngine engine = new ToolExecutionEngine(registry, approvalService, objectMapper);

        Map<String, Object> result = engine.executeTool("dummy_refund", Map.of("orderNumber", "1001", "amount", 50), "RefundAgent", "operator");

        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.get("status"));
        assertEquals(mockReq.getId(), result.get("actionRequestId"));
        verify(approvalService, times(1)).createActionRequest(eq("RefundAgent"), eq("dummy_refund"), eq(ToolRiskLevel.HIGH), any(), eq("operator"));
    }
}
