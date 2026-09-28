package com.shopai.tool.controller;

import com.shopai.auth.security.UserPrincipal;
import com.shopai.common.dto.ApiResponse;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolExecutionEngine;
import com.shopai.tool.core.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/tools")
@Tag(name = "Tool Registry", description = "AI Agent Tool Discovery and Execution Engine")
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final ToolExecutionEngine toolExecutionEngine;

    public ToolController(ToolRegistry toolRegistry, ToolExecutionEngine toolExecutionEngine) {
        this.toolRegistry = toolRegistry;
        this.toolExecutionEngine = toolExecutionEngine;
    }

    public record ToolDto(
            String name,
            String description,
            String requiredPermission,
            String riskLevel,
            boolean requiresHumanApproval,
            Map<String, Object> parametersSchema
    ) {
        public static ToolDto from(Tool t) {
            return new ToolDto(
                    t.getName(),
                    t.getDescription(),
                    t.getRequiredPermission(),
                    t.getRiskLevel().name(),
                    t.requiresHumanApproval(),
                    t.getParametersSchema()
            );
        }
    }

    public record ExecuteToolRequest(
            String toolName,
            String agentName,
            Map<String, Object> parameters
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "List all registered tools available to AI agents")
    public ResponseEntity<ApiResponse<List<ToolDto>>> getAllTools() {
        List<ToolDto> tools = toolRegistry.getAllTools().stream()
                .map(ToolDto::from)
                .sorted(Comparator.comparing(ToolDto::name))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(tools));
    }

    @PostMapping("/execute")
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Execute a tool directly or route to human approval if high risk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeTool(
            @RequestBody ExecuteToolRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (request.toolName() == null || request.toolName().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(com.shopai.common.dto.ErrorResponse.of("INVALID_TOOL", "Tool name is required")));
        }

        String user = userPrincipal != null ? userPrincipal.getUsername() : "merchant_operator";
        String agent = request.agentName() != null ? request.agentName() : "ToolExplorer";

        Map<String, Object> result = toolExecutionEngine.executeTool(
                request.toolName(),
                request.parameters() != null ? request.parameters() : Map.of(),
                agent,
                user
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
