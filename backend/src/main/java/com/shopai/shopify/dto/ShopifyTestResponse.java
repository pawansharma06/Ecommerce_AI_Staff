package com.shopai.shopify.dto;

import java.util.Map;

public record ShopifyTestResponse(
        boolean success,
        String message,
        Map<String, Object> shopDetails
) {}
