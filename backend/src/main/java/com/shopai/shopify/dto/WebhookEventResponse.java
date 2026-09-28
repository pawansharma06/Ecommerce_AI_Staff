package com.shopai.shopify.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookEventResponse(
        UUID id,
        String topic,
        String shopifyWebhookId,
        String shopDomain,
        String apiVersion,
        String status,
        int attempts,
        String errorMessage,
        Instant processedAt,
        Instant createdAt
) {}
