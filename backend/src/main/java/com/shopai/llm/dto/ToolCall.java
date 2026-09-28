package com.shopai.llm.dto;

public record ToolCall(
        String id,
        String toolName,
        String argumentsJson
) {}