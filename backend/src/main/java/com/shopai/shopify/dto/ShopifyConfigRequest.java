package com.shopai.shopify.dto;

import jakarta.validation.constraints.NotBlank;

public record ShopifyConfigRequest(
        @NotBlank(message = "Shop domain is required (e.g. my-store.myshopify.com)")
        String shopDomain,

        @NotBlank(message = "Admin API access token is required (shpat_...)")
        String adminAccessToken,

        String webhookSecret,
        String apiVersion
) {}
