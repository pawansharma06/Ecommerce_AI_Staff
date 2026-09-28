package com.shopai.rag.dto;

public record RagStatsResponse(
        long totalDocuments,
        long totalChunks,
        long productDocuments,
        long orderDocuments,
        long checkoutDocuments,
        long policyDocuments,
        long customDocuments,
        long totalCustomerMemories,
        String embeddingProvider,
        int vectorDimensions,
        String indexType
) {}
