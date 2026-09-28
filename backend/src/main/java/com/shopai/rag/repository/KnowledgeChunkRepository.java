package com.shopai.rag.repository;

import com.shopai.rag.domain.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    @Modifying
    @Transactional
    @Query("DELETE FROM KnowledgeChunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO knowledge_chunks (id, document_id, chunk_index, content, token_count, embedding, customer_id, customer_email, metadata, created_at)
        VALUES (:id, :documentId, :chunkIndex, :content, :tokenCount, cast(:embedding as vector), :customerId, :customerEmail, cast(:metadata as jsonb), NOW())
    """, nativeQuery = true)
    void insertChunkWithEmbedding(
            @Param("id") UUID id,
            @Param("documentId") UUID documentId,
            @Param("chunkIndex") int chunkIndex,
            @Param("content") String content,
            @Param("tokenCount") int tokenCount,
            @Param("embedding") String embedding,
            @Param("customerId") Long customerId,
            @Param("customerEmail") String customerEmail,
            @Param("metadata") String metadata
    );

    @Query(value = """
        SELECT cast(c.id as text), cast(c.document_id as text), c.chunk_index, c.content, c.token_count, 
               c.customer_email, cast(c.metadata as text),
               (1 - (c.embedding <=> cast(:embedding as vector))) AS score,
               d.document_type, d.title
        FROM knowledge_chunks c
        JOIN knowledge_documents d ON c.document_id = d.id
        WHERE (cast(:customerEmail as text) IS NULL OR c.customer_email = cast(:customerEmail as text) OR c.customer_email IS NULL)
          AND (cast(:docType as text) IS NULL OR d.document_type = cast(:docType as text))
          AND (1 - (c.embedding <=> cast(:embedding as vector))) >= :minScore
        ORDER BY c.embedding <=> cast(:embedding as vector)
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> searchSimilarChunks(
            @Param("embedding") String embedding,
            @Param("customerEmail") String customerEmail,
            @Param("docType") String docType,
            @Param("minScore") double minScore,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT cast(c.id as text), cast(c.document_id as text), c.chunk_index, c.content, c.token_count, 
               c.customer_email, cast(c.metadata as text),
               (1 - (c.embedding <=> cast(:embedding as vector))) AS score,
               d.document_type, d.title
        FROM knowledge_chunks c
        JOIN knowledge_documents d ON c.document_id = d.id
        WHERE c.customer_email = :customerEmail
          AND (1 - (c.embedding <=> cast(:embedding as vector))) >= :minScore
        ORDER BY c.embedding <=> cast(:embedding as vector)
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> searchCustomerMemoryChunks(
            @Param("embedding") String embedding,
            @Param("customerEmail") String customerEmail,
            @Param("minScore") double minScore,
            @Param("limit") int limit
    );
}
