package com.shopai.rag.repository;

import com.shopai.rag.domain.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByDocumentTypeAndSourceId(String documentType, String sourceId);

    List<KnowledgeDocument> findByCustomerEmail(String customerEmail);

    @Query("""
        SELECT d FROM KnowledgeDocument d
        WHERE (CAST(:search AS string) IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(d.content) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:docType AS string) IS NULL OR d.documentType = :docType)
    """)
    Page<KnowledgeDocument> findWithFilters(
            @Param("search") String search,
            @Param("docType") String docType,
            Pageable pageable
    );

    long countByDocumentType(String documentType);
}
