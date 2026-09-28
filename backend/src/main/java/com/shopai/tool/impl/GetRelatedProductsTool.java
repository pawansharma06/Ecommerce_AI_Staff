package com.shopai.tool.impl;

import com.shopai.graph.service.GraphRetriever;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetRelatedProductsTool implements Tool {

    private final GraphRetriever graphRetriever;

    public GetRelatedProductsTool(GraphRetriever graphRetriever) {
        this.graphRetriever = graphRetriever;
    }

    @Override
    public String getName() {
        return "get_related_products";
    }

    @Override
    public String getDescription() {
        return "Retrieve related, complementary, or alternative products from the Commerce Knowledge Graph.";
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
                        "productId", Map.of("type", "string", "description", "The product ID or handle to find related products for"),
                        "limit", Map.of("type", "integer", "description", "Maximum number of recommendations (default 5)")
                ),
                "required", List.of("productId")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        int limit = params.get("limit") instanceof Number n ? n.intValue() : 5;
        return graphRetriever.findRelatedProducts(productId, limit);
    }
}
