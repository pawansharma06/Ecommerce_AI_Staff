package com.shopai.agent.controller;

import com.shopai.agent.dto.*;
import com.shopai.agent.service.AgentRuntimeService;
import com.shopai.auth.security.UserPrincipal;
import com.shopai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Agent Conversations", description = "Autonomous AI Agent Conversation Management and Execution Runtime")
public class ConversationController {

    private final AgentRuntimeService runtimeService;

    public ConversationController(AgentRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'order.read', 'ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_VIEWER')")
    @Operation(summary = "Create a new conversation session")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String user = userPrincipal != null ? userPrincipal.getUsername() : "anonymous";
        ConversationResponse resp = runtimeService.createConversation(request, user);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'order.read', 'ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_VIEWER')")
    @Operation(summary = "List conversation sessions with filters")
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> listConversations(
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ConversationResponse> results = runtimeService.listConversations(agentType, customerEmail, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'order.read', 'ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_VIEWER')")
    @Operation(summary = "Get conversation session details with full message history")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(@PathVariable UUID id) {
        ConversationResponse resp = runtimeService.getConversation(id);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'order.read', 'ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_VIEWER')")
    @Operation(summary = "Send message to conversation and execute multi-turn agent reasoning loop")
    public ResponseEntity<ApiResponse<AgentTurnResult>> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String user = userPrincipal != null ? userPrincipal.getUsername() : "user";
        AgentTurnResult result = runtimeService.processTurn(id, request.content(), request.customerEmail(), user);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('settings.manage', 'ROLE_ADMIN')")
    @Operation(summary = "Delete conversation session")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteConversation(@PathVariable UUID id) {
        runtimeService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Conversation deleted successfully")));
    }
}
