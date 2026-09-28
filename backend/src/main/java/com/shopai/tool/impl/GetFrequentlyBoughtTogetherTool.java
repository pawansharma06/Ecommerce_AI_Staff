package com.shopai.tool.impl;

import com.shopai.graph.service.GraphRetriever;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetFrequentlyBoughtTogetherTool implements Tool {

    private final GraphRetriever graphRetriever;

    public GetFrequentlyBoughtTogetherTool(GraphRetriever graphRetriever) {
        this.graphRetriever = graphRetriever;
    }

    @Override
    public String getName() {
        return "get_frequently_bought_together";
    }

    @Override
    public String getDescription() {
        return "Find items that other customers frequently purchased together with a specified product based on real order graph history.";
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
                        "productId", Map.of("type", "string", "description", "The product ID or handle"),
                        "limit", Map.of("type", "integer", "description", "Maximum items to return (default 5)")
                ),
                "required", List.of("productId")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        int limit = params.get("limit") instanceof Number n ? n.intValue() : 5;
        return graphRetriever.findFrequentlyBoughtProducts(productId, limit);
    }
}
