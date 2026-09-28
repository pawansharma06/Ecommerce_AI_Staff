package com.shopai.tool.impl;

import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.rag.dto.SearchRequest;
import com.shopai.rag.dto.SearchResultResponse;
import com.shopai.rag.service.VectorSearchService;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SearchProductsTool implements Tool {

    private final ProductRepository productRepository;
    private final VectorSearchService vectorSearchService;

    public SearchProductsTool(ProductRepository productRepository, VectorSearchService vectorSearchService) {
        this.productRepository = productRepository;
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public String getName() {
        return "search_products";
    }

    @Override
    public String getDescription() {
        return "Search store products by keyword, category, or semantic natural language query.";
    }

    @Override
    public String getRequiredPermission() {
        return "product.read";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Search query or product keywords (e.g. 'winter boots', 'snowboard')"),
                        "limit", Map.of("type", "integer", "description", "Maximum results to return (default 5)")
                ),
                "required", List.of("query")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) return Collections.emptyList();
        int limit = params.get("limit") instanceof Number n ? n.intValue() : 5;

        // Semantic vector search on products
        List<VectorSearchService.SearchResultItem> vecResults = vectorSearchService.search(query, null, "PRODUCT", 0.30, limit);
        if (!vecResults.isEmpty()) {
            return vecResults.stream().map(r -> Map.of(
                    "title", r.documentTitle(),
                    "snippet", r.content(),
                    "score", r.similarityScore(),
                    "metadata", r.metadata()
            )).toList();
        }

        // Keyword fallback in PostgreSQL
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                             (p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase())))
                .limit(limit)
                .toList();

        return products.stream().map(p -> Map.of(
                "id", p.getId(),
                "shopifyProductId", p.getShopifyProductId(),
                "title", p.getTitle(),
                "handle", p.getHandle(),
                "totalInventory", p.getTotalInventory(),
                "status", p.getStatus()
        )).toList();
    }
}