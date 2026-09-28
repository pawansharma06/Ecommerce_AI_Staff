package com.shopai.llm.dto;

import java.util.List;

public record LlmPrompt(
        String systemPrompt,
        List<LlmMessage> messages,
        Double temperature,
        Integer maxTokens
) {
    public static LlmPrompt of(String systemPrompt, String userMessage) {
        return new LlmPrompt(systemPrompt, List.of(LlmMessage.user(userMessage)), 0.2, 1024);
    }
}