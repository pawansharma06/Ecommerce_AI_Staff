package com.shopai.shopify.dto;

import java.time.Instant;
import java.util.UUID;

public record ShopifyConfigResponse(
        UUID id,
        String shopDomain,
        boolean isAccessTokenConfigured,
        boolean isWebhookSecretConfigured,
        String apiVersion,
        String shopName,
        String shopOwner,
        String email,
        String currency,
        String timezone,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
