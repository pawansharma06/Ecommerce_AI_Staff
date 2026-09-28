package com.shopai.customer.repository;

import com.shopai.customer.domain.CustomerMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerMemoryRepository extends JpaRepository<CustomerMemory, UUID> {

    List<CustomerMemory> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    Optional<CustomerMemory> findByCustomerEmailAndMemoryKey(String customerEmail, String memoryKey);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO customer_memories (id, customer_id, customer_email, category, memory_key, memory_value, confidence_score, embedding, created_at, updated_at)
        VALUES (:id, :customerId, :customerEmail, :category, :memoryKey, :memoryValue, :confidenceScore, cast(:embedding as vector), NOW(), NOW())
    """, nativeQuery = true)
    void insertMemoryWithEmbedding(
            @Param("id") UUID id,
            @Param("customerId") Long customerId,
            @Param("customerEmail") String customerEmail,
            @Param("category") String category,
            @Param("memoryKey") String memoryKey,
            @Param("memoryValue") String memoryValue,
            @Param("confidenceScore") BigDecimal confidenceScore,
            @Param("embedding") String embedding
    );

    @Query(value = """
        SELECT cast(m.id as text), m.customer_email, m.category, m.memory_key, m.memory_value, m.confidence_score,
               (1 - (m.embedding <=> cast(:embedding as vector))) AS score
        FROM customer_memories m
        WHERE m.customer_email = :customerEmail
          AND (1 - (m.embedding <=> cast(:embedding as vector))) >= :minScore
        ORDER BY m.embedding <=> cast(:embedding as vector)
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> searchSimilarMemories(
            @Param("embedding") String embedding,
            @Param("customerEmail") String customerEmail,
            @Param("minScore") double minScore,
            @Param("limit") int limit
    );
}
