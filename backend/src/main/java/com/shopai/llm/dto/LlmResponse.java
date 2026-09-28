package com.shopai.llm.dto;

import java.util.Collections;
import java.util.List;

public record LlmResponse(
        String content,
        List<ToolCall> toolCalls,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        String finishReason,
        String providerName,
        String modelName
) {
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public static LlmResponse text(String content, int promptTokens, int completionTokens, String provider, String model) {
        return new LlmResponse(content, Collections.emptyList(), promptTokens, completionTokens, promptTokens + completionTokens, "stop", provider, model);
    }
}