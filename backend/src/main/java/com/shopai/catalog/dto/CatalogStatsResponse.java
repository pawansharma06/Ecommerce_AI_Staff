package com.shopai.catalog.dto;

public record CatalogStatsResponse(
        long totalProducts,
        long activeProducts,
        long draftProducts,
        long archivedProducts,
        long totalVariants,
        long lowStockVariants
) {}
