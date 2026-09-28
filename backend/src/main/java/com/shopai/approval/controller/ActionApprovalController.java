package com.shopai.approval.controller;

import com.shopai.approval.dto.ActionApprovalResponse;
import com.shopai.approval.service.ActionApprovalService;
import com.shopai.auth.security.UserPrincipal;
import com.shopai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Action Approvals", description = "Human-in-the-Loop AI Action Approval Queue and Execution Gate")
public class ActionApprovalController {

    private final ActionApprovalService approvalService;

    public ActionApprovalController(ActionApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    public record RejectRequest(String reason) {}

    @GetMapping
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Get action approval requests with status filter and pagination")
    public ResponseEntity<ApiResponse<Page<ActionApprovalResponse>>> getActionRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ActionApprovalResponse> result = approvalService.getActionRequests(status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Get total count of pending action approval requests")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount() {
        long count = approvalService.getPendingCount();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("pendingCount", count)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('settings.manage', 'ROLE_ADMIN')")
    @Operation(summary = "Approve and execute a pending high-risk AI action")
    public ResponseEntity<ApiResponse<ActionApprovalResponse>> approveAction(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String approvedBy = userPrincipal != null ? userPrincipal.getUsername() : "merchant_operator";
        ActionApprovalResponse resp = approvalService.approveAndExecute(id, approvedBy);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('settings.manage', 'ROLE_ADMIN')")
    @Operation(summary = "Reject a pending AI action request with reason")
    public ResponseEntity<ApiResponse<ActionApprovalResponse>> rejectAction(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectRequest body,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String rejectedBy = userPrincipal != null ? userPrincipal.getUsername() : "merchant_operator";
        String reason = body != null && body.reason() != null ? body.reason() : "Rejected by merchant operator";
        ActionApprovalResponse resp = approvalService.reject(id, reason, rejectedBy);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }
}
