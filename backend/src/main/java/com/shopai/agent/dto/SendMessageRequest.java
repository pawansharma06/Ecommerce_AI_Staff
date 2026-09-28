package com.shopai.agent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record SendMessageRequest(
        @NotBlank(message = "content is required")
        String content,
        String customerEmail,
        Map<String, Object> metadata
) {}
