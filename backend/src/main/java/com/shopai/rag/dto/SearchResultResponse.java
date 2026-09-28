package com.shopai.rag.dto;

import java.util.Map;

public record SearchResultResponse(
        String id,
        String documentId,
        String documentTitle,
        String documentType,
        String content,
        int tokenCount,
        String customerEmail,
        double similarityScore,
        Map<String, Object> metadata
) {}
