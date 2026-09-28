package com.shopai.catalog.dto;

import com.shopai.catalog.domain.Product;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProductResponse(
        UUID id,
        Long shopifyProductId,
        String title,
        String handle,
        String description,
        String vendor,
        String productType,
        List<String> tags,
        String status,
        Integer totalInventory,
        Instant publishedAt,
        Instant shopifyCreatedAt,
        Instant shopifyUpdatedAt,
        Instant syncedAt,
        List<ProductVariantResponse> variants,
        List<CollectionResponse> collections
) {
    public static ProductResponse from(Product p) {
        List<ProductVariantResponse> variantResponses = p.getVariants() != null
                ? p.getVariants().stream().map(ProductVariantResponse::from).collect(Collectors.toList())
                : List.of();

        List<CollectionResponse> collectionResponses = p.getCollections() != null
                ? p.getCollections().stream().map(CollectionResponse::from).collect(Collectors.toList())
                : List.of();

        List<String> tagList = p.getTags() != null ? Arrays.asList(p.getTags()) : List.of();

        return new ProductResponse(
                p.getId(),
                p.getShopifyProductId(),
                p.getTitle(),
                p.getHandle(),
                p.getDescription(),
                p.getVendor(),
                p.getProductType(),
                tagList,
                p.getStatus(),
                p.getTotalInventory(),
                p.getPublishedAt(),
                p.getShopifyCreatedAt(),
                p.getShopifyUpdatedAt(),
                p.getSyncedAt(),
                variantResponses,
                collectionResponses
        );
    }
}
