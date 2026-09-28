package com.shopai.llm.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.common.dto.ErrorResponse;
import com.shopai.llm.domain.LlmUsageLog;
import com.shopai.llm.dto.LlmMessage;
import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.repository.LlmUsageLogRepository;
import com.shopai.llm.service.LlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/llm")
@Tag(name = "LLM Subsystem", description = "Multi-Provider LLM Gateway, Status, and Usage Telemetry")
public class LlmController {

    private final LlmService llmService;
    private final LlmUsageLogRepository usageLogRepository;

    public LlmController(LlmService llmService, LlmUsageLogRepository usageLogRepository) {
        this.llmService = llmService;
        this.usageLogRepository = usageLogRepository;
    }

    public record GeneratePromptRequest(
            String systemPrompt,
            String userPrompt,
            String agentName,
            Double temperature
    ) {}

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Get active LLM provider configuration and aggregate usage metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(llmService.getStatus()));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Test LLM prompt generation with active provider")
    public ResponseEntity<ApiResponse<LlmResponse>> generate(@RequestBody GeneratePromptRequest request) {
        if (request.userPrompt() == null || request.userPrompt().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorResponse.of("INVALID_PROMPT", "userPrompt is required")));
        }

        LlmPrompt prompt = new LlmPrompt(
                request.systemPrompt() != null ? request.systemPrompt() : "You are ShopAI assistant.",
                List.of(LlmMessage.user(request.userPrompt())),
                request.temperature() != null ? request.temperature() : 0.7,
                1024
        );

        LlmResponse response = llmService.generateChat(prompt, request.agentName() != null ? request.agentName() : "TestConsole");
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('agent.execute', 'settings.manage', 'ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Get recent LLM token usage and latency logs")
    public ResponseEntity<ApiResponse<List<LlmUsageLog>>> getRecentLogs() {
        List<LlmUsageLog> logs = usageLogRepository.findTop50ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
