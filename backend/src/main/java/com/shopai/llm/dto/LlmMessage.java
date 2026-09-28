package com.shopai.llm.dto;

import java.util.List;

public record LlmMessage(
        String role, // system, user, assistant, tool
        String content,
        String name,
        List<ToolCall> toolCalls,
        String toolCallId
) {
    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null, null);
    }
    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null, null);
    }
    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null, null, null);
    }
    public static LlmMessage assistantWithTools(String content, List<ToolCall> toolCalls) {
        return new LlmMessage("assistant", content, null, toolCalls, null);
    }
    public static LlmMessage toolResult(String toolCallId, String toolName, String content) {
        return new LlmMessage("tool", content, toolName, null, toolCallId);
    }
}