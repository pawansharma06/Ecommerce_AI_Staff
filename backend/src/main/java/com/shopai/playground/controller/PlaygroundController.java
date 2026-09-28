package com.shopai.playground.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.playground.dto.PlaygroundDtos.*;
import com.shopai.playground.service.PlaygroundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/playground")
@Tag(name = "Agent Playground & Tracing", description = "Interactive workbench for agent reasoning simulation, tool tracing, and prompt inspection")
public class PlaygroundController {

    private final PlaygroundService playgroundService;

    public PlaygroundController(PlaygroundService playgroundService) {
        this.playgroundService = playgroundService;
    }

    @PostMapping("/trace")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Execute a sandboxed diagnostic agent turn with fine-grained step-by-step telemetry")
    public ResponseEntity<ApiResponse<PlaygroundTraceResponse>> executeTrace(
            @RequestBody PlaygroundTraceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(playgroundService.executeTrace(request)));
    }

    @PostMapping("/preview-prompt")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Preview rendered agent system prompt with dynamic RAG & customer memory injection")
    public ResponseEntity<ApiResponse<PromptPreviewResponse>> previewPrompt(
            @RequestBody PromptPreviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(playgroundService.previewPrompt(request)));
    }
}
