package com.shopai.customer.service;

import com.shopai.customer.domain.CustomerMemory;
import com.shopai.customer.repository.CustomerMemoryRepository;
import com.shopai.rag.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CustomerMemoryService {

    private static final Logger log = LoggerFactory.getLogger(CustomerMemoryService.class);

    private final CustomerMemoryRepository memoryRepository;
    private final EmbeddingService embeddingService;

    public CustomerMemoryService(CustomerMemoryRepository memoryRepository, EmbeddingService embeddingService) {
        this.memoryRepository = memoryRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional(readOnly = true)
    public List<CustomerMemory> getMemoriesByEmail(String email) {
        if (email == null || email.isBlank()) return Collections.emptyList();
        return memoryRepository.findByCustomerEmailOrderByCreatedAtDesc(email.trim().toLowerCase());
    }

    @Transactional
    public CustomerMemory recordMemory(String email, String category, String key, String value, BigDecimal confidence) {
        if (email == null || email.isBlank() || key == null || value == null) {
            throw new IllegalArgumentException("Customer email, key, and value are required");
        }

        String cleanEmail = email.trim().toLowerCase();
        String embeddingStr = embeddingService.getEmbeddingAsVectorString(key + ": " + value);

        Optional<CustomerMemory> existing = memoryRepository.findByCustomerEmailAndMemoryKey(cleanEmail, key);
        if (existing.isPresent()) {
            CustomerMemory mem = existing.get();
            mem.setCategory(category != null ? category : "PREFERENCE");
            mem.setMemoryValue(value);
            mem.setConfidenceScore(confidence != null ? confidence : BigDecimal.ONE);
            mem.setUpdatedAt(java.time.Instant.now());
            mem = memoryRepository.save(mem);

            // Update vector embedding
            memoryRepository.delete(mem);
            memoryRepository.insertMemoryWithEmbedding(
                    mem.getId(),
                    mem.getCustomerId(),
                    cleanEmail,
                    mem.getCategory(),
                    key,
                    value,
                    mem.getConfidenceScore(),
                    embeddingStr
            );
            return mem;
        }

        UUID id = UUID.randomUUID();
        CustomerMemory memory = new CustomerMemory(cleanEmail, category != null ? category : "PREFERENCE", key, value);
        memory.setId(id);
        if (confidence != null) memory.setConfidenceScore(confidence);

        memoryRepository.insertMemoryWithEmbedding(
                id,
                null,
                cleanEmail,
                memory.getCategory(),
                key,
                value,
                memory.getConfidenceScore(),
                embeddingStr
        );

        log.info("Recorded customer memory for {}: [{}] = {}", cleanEmail, key, value);
        return memory;
    }

    @Transactional
    public void deleteMemory(UUID id) {
        memoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchCustomerMemories(String email, String query, double minScore, int limit) {
        if (email == null || email.isBlank() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String queryEmbedding = embeddingService.getEmbeddingAsVectorString(query);
        List<Object[]> rows = memoryRepository.searchSimilarMemories(queryEmbedding, email.trim().toLowerCase(), minScore, limit);

        List<Map<String, Object>> results = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("customerEmail", row[1]);
            map.put("category", row[2]);
            map.put("memoryKey", row[3]);
            map.put("memoryValue", row[4]);
            map.put("confidenceScore", row[5]);
            map.put("similarityScore", row[6]);
            results.add(map);
        }
        return results;
    }
}
