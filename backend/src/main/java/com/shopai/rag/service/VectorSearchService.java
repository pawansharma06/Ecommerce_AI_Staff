package com.shopai.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.rag.embedding.EmbeddingService;
import com.shopai.rag.repository.KnowledgeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final KnowledgeChunkRepository chunkRepository;
    private final CustomerMemoryService customerMemoryService;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public VectorSearchService(
            KnowledgeChunkRepository chunkRepository,
            CustomerMemoryService customerMemoryService,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper
    ) {
        this.chunkRepository = chunkRepository;
        this.customerMemoryService = customerMemoryService;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    public record SearchResultItem(
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

    @Transactional(readOnly = true)
    public List<SearchResultItem> search(String query, String customerEmail, String docType, double minScore, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        int fetchLimit = limit > 0 ? limit : 5;
        double threshold = minScore >= 0.0 ? minScore : 0.40;
        String cleanEmail = (customerEmail != null && !customerEmail.isBlank()) ? customerEmail.trim().toLowerCase() : null;
        String cleanDocType = (docType != null && !docType.isBlank() && !"ALL".equalsIgnoreCase(docType)) ? docType.trim().toUpperCase() : null;

        String queryEmbedding = embeddingService.getEmbeddingAsVectorString(query);
        List<Object[]> rows = chunkRepository.searchSimilarChunks(queryEmbedding, cleanEmail, cleanDocType, threshold, fetchLimit);

        List<SearchResultItem> results = new ArrayList<>();
        for (Object[] row : rows) {
            String chunkId = (String) row[0];
            String docId = (String) row[1];
            int chunkIndex = row[2] != null ? ((Number) row[2]).intValue() : 0;
            String content = (String) row[3];
            int tokenCount = row[4] != null ? ((Number) row[4]).intValue() : 0;
            String email = (String) row[5];
            String metadataJson = (String) row[6];
            double score = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;
            String type = (String) row[8];
            String title = (String) row[9];

            Map<String, Object> meta = new HashMap<>();
            if (metadataJson != null && !metadataJson.isBlank()) {
                try {
                    meta = objectMapper.readValue(metadataJson, Map.class);
                } catch (Exception e) {
                    meta.put("raw", metadataJson);
                }
            }
            meta.put("chunkIndex", chunkIndex);

            results.add(new SearchResultItem(
                    chunkId,
                    docId,
                    title != null ? title : "Knowledge Document",
                    type != null ? type : "CUSTOM",
                    content,
                    tokenCount,
                    email,
                    score,
                    meta
            ));
        }

        log.debug("Vector search for '{}' (email: {}) returned {} items", query, cleanEmail, results.size());
        return results;
    }
}
