package com.shopai.catalog.dto;

import com.shopai.catalog.domain.Collection;
import java.time.Instant;
import java.util.UUID;

public record CollectionResponse(
        UUID id,
        Long shopifyCollectionId,
        String title,
        String handle,
        String description,
        String collectionType,
        Instant syncedAt
) {
    public static CollectionResponse from(Collection c) {
        return new CollectionResponse(
                c.getId(),
                c.getShopifyCollectionId(),
                c.getTitle(),
                c.getHandle(),
                c.getDescription(),
                c.getCollectionType(),
                c.getSyncedAt()
        );
    }
}
