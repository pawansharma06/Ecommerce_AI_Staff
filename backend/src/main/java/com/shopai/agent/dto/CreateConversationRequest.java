package com.shopai.agent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CreateConversationRequest(
        String title,
        @NotBlank(message = "agentType is required")
        String agentType, // CUSTOMER_SUPPORT, ADMIN_COPILOT
        String channel,   // WEB_CHAT, ADMIN_COPILOT, STOREFRONT_WIDGET
        String customerEmail,
        Long customerId,
        Map<String, Object> metadata
) {}
