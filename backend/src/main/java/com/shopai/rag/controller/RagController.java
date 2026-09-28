package com.shopai.rag.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.customer.repository.CustomerMemoryRepository;
import com.shopai.rag.domain.KnowledgeDocument;
import com.shopai.rag.dto.*;
import com.shopai.rag.embedding.EmbeddingService;
import com.shopai.rag.repository.KnowledgeChunkRepository;
import com.shopai.rag.repository.KnowledgeDocumentRepository;
import com.shopai.rag.service.KnowledgeIndexService;
import com.shopai.rag.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "Vector RAG & Knowledge Base", description = "Semantic Vector Search and Store Knowledge Ingestion")
public class RagController {

    private final VectorSearchService vectorSearchService;
    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final CustomerMemoryRepository customerMemoryRepository;
    private final EmbeddingService embeddingService;

    public RagController(
            VectorSearchService vectorSearchService,
            KnowledgeIndexService knowledgeIndexService,
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkRepository chunkRepository,
            CustomerMemoryRepository customerMemoryRepository,
            EmbeddingService embeddingService
    ) {
        this.vectorSearchService = vectorSearchService;
        this.knowledgeIndexService = knowledgeIndexService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.customerMemoryRepository = customerMemoryRepository;
        this.embeddingService = embeddingService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('product.read') or hasAuthority('order.read')")
    @Operation(summary = "Perform semantic vector similarity search")
    public ResponseEntity<ApiResponse<List<SearchResultResponse>>> search(@Valid @RequestBody SearchRequest request) {
        double minScore = request.minScore() != null ? request.minScore() : 0.50;
        int limit = request.limit() != null ? request.limit() : 5;

        List<VectorSearchService.SearchResultItem> items = vectorSearchService.search(
                request.query(),
                request.customerEmail(),
                request.documentType(),
                minScore,
                limit
        );

        List<SearchResultResponse> responses = items.stream()
                .map(i -> new SearchResultResponse(
                        i.id(),
                        i.documentId(),
                        i.documentTitle(),
                        i.documentType(),
                        i.content(),
                        i.tokenCount(),
                        i.customerEmail(),
                        i.similarityScore(),
                        i.metadata()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/index")
    @PreAuthorize("hasAuthority('integration.manage')")
    @Operation(summary = "Trigger full asynchronous vector re-indexing")
    public ResponseEntity<ApiResponse<Map<String, String>>> triggerReindexing() {
        knowledgeIndexService.reindexAllKnowledgeAsync();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Full vector re-indexing triggered in background")));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('product.read')")
    @Operation(summary = "Get Vector RAG and Knowledge Base statistics")
    public ResponseEntity<ApiResponse<RagStatsResponse>> getStats() {
        long totalDocs = documentRepository.count();
        long totalChunks = chunkRepository.count();
        long prodDocs = documentRepository.countByDocumentType("PRODUCT");
        long orderDocs = documentRepository.countByDocumentType("ORDER_HISTORY");
        long checkoutDocs = documentRepository.countByDocumentType("ABANDONED_CHECKOUT");
        long policyDocs = documentRepository.countByDocumentType("POLICY");
        long customDocs = documentRepository.countByDocumentType("CUSTOM") + documentRepository.countByDocumentType("FAQ");
        long custMemories = customerMemoryRepository.count();

        RagStatsResponse stats = new RagStatsResponse(
                totalDocs,
                totalChunks,
                prodDocs,
                orderDocs,
                checkoutDocs,
                policyDocs,
                customDocs,
                custMemories,
                embeddingService.getActiveProviderName(),
                1536,
                "HNSW (vector_cosine_ops)"
        );

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('product.read')")
    @Operation(summary = "List knowledge documents with pagination and filtering")
    public ResponseEntity<ApiResponse<Page<KnowledgeDocumentResponse>>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String documentType
    ) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanType = (documentType != null && !documentType.isBlank() && !"ALL".equalsIgnoreCase(documentType)) ? documentType.trim().toUpperCase() : null;

        Page<KnowledgeDocumentResponse> result = documentRepository.findWithFilters(
                cleanSearch,
                cleanType,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        ).map(KnowledgeDocumentResponse::from);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAuthority('integration.manage')")
    @Operation(summary = "Create custom knowledge document and index into vector space")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        KnowledgeDocument doc = new KnowledgeDocument(
                request.title(),
                request.content(),
                request.documentType() != null ? request.documentType().toUpperCase() : "CUSTOM",
                UUID.randomUUID().toString()
        );
        doc.setCustomerEmail(request.customerEmail());
        knowledgeIndexService.indexCustomDocument(doc);
        return ResponseEntity.ok(ApiResponse.ok(KnowledgeDocumentResponse.from(doc)));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAuthority('integration.manage')")
    @Operation(summary = "Delete knowledge document and associated vector chunks")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteDocument(@PathVariable UUID id) {
        documentRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Document deleted successfully")));
    }
}
