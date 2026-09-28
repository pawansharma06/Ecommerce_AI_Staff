package com.shopai.rag.dto;

import com.shopai.rag.domain.KnowledgeDocument;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentResponse(
        UUID id,
        String title,
        String content,
        String documentType,
        String sourceId,
        Long customerId,
        String customerEmail,
        String status,
        String metadata,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument d) {
        return new KnowledgeDocumentResponse(
                d.getId(),
                d.getTitle(),
                d.getContent(),
                d.getDocumentType(),
                d.getSourceId(),
                d.getCustomerId(),
                d.getCustomerEmail(),
                d.getStatus(),
                d.getMetadata(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
