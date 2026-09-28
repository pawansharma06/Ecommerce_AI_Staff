package com.shopai.catalog.dto;

import com.shopai.catalog.domain.ProductVariant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        Long shopifyVariantId,
        String title,
        String sku,
        String barcode,
        BigDecimal price,
        BigDecimal compareAtPrice,
        Integer inventoryQuantity,
        Integer position,
        String imageUrl,
        Boolean requiresShipping,
        Instant updatedAt
) {
    public static ProductVariantResponse from(ProductVariant v) {
        return new ProductVariantResponse(
                v.getId(),
                v.getShopifyVariantId(),
                v.getTitle(),
                v.getSku(),
                v.getBarcode(),
                v.getPrice(),
                v.getCompareAtPrice(),
                v.getInventoryQuantity(),
                v.getPosition(),
                v.getImageUrl(),
                v.getRequiresShipping(),
                v.getUpdatedAt()
        );
    }
}
