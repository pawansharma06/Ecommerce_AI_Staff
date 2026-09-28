package com.shopai.tool.impl;

import com.shopai.rag.service.VectorSearchService;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetStorePolicyTool implements Tool {

    private final VectorSearchService vectorSearchService;

    public GetStorePolicyTool(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public String getName() {
        return "get_store_policy";
    }

    @Override
    public String getDescription() {
        return "Search and retrieve official store policies including returns, exchanges, refunds, shipping rules, and privacy.";
    }

    @Override
    public String getRequiredPermission() {
        return "policy.read";
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
                        "query", Map.of("type", "string", "description", "Policy question or topic (e.g. 'return window', 'international shipping', 'damaged goods')")
                ),
                "required", List.of("query")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<VectorSearchService.SearchResultItem> policyChunks = vectorSearchService.search(query, null, "POLICY", 0.20, 3);
        if (policyChunks.isEmpty()) {
            // General vector search fallback across all docs
            policyChunks = vectorSearchService.search(query, null, null, 0.20, 3);
        }

        return policyChunks.stream().map(p -> Map.of(
                "title", p.documentTitle(),
                "snippet", p.content(),
                "relevanceScore", p.similarityScore(),
                "docType", p.documentType() != null ? p.documentType() : "POLICY"
        )).toList();
    }
}
