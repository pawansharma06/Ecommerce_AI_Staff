package com.shopai.playground.dto;

import java.util.List;
import java.util.Map;

public class PlaygroundDtos {

    public record PlaygroundTraceRequest(
            String agentType,
            String channel,
            String customerEmail,
            String userQuery,
            Double temperature,
            Integer maxSteps
    ) {}

    public record ToolTraceItem(
            String toolCallId,
            String toolName,
            Map<String, Object> arguments,
            String resultOutput,
            boolean requiresApproval,
            String status,
            long executionMs
    ) {}

    public record TraceStepItem(
            int stepIndex,
            String assistantThought,
            List<ToolTraceItem> toolCalls,
            long stepLatencyMs,
            int stepTokens
    ) {}

    public record InjectedMemoryItem(
            String category,
            String key,
            String value
    ) {}

    public record InjectedRagChunk(
            String documentTitle,
            String contentSnippet,
            double similarityScore
    ) {}

    public record InjectedContextSummary(
            String systemPrompt,
            List<InjectedMemoryItem> customerMemories,
            List<InjectedRagChunk> ragChunks,
            List<Map<String, Object>> graphRelationships
    ) {}

    public record PlaygroundTraceResponse(
            String agentType,
            String channel,
            String userQuery,
            InjectedContextSummary injectedContext,
            List<TraceStepItem> traceSteps,
            String finalResponse,
            int totalTokens,
            int totalToolsExecuted,
            long totalLatencyMs
    ) {}

    public record PromptPreviewRequest(
            String agentType,
            String customerEmail,
            String userQuery
    ) {}

    public record PromptPreviewResponse(
            String agentType,
            String customerEmail,
            String renderedPrompt,
            int estimatedTokens,
            List<InjectedMemoryItem> injectedMemories,
            List<InjectedRagChunk> injectedRagChunks
    ) {}
}
