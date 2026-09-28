package com.shopai.tool.impl;

import com.shopai.graph.service.GraphRetriever;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetCustomerPurchaseGraphTool implements Tool {

    private final GraphRetriever graphRetriever;

    public GetCustomerPurchaseGraphTool(GraphRetriever graphRetriever) {
        this.graphRetriever = graphRetriever;
    }

    @Override
    public String getName() {
        return "get_customer_purchase_graph";
    }

    @Override
    public String getDescription() {
        return "Retrieve the full historical purchase entity graph for a customer by their email address.";
    }

    @Override
    public String getRequiredPermission() {
        return "customer.read";
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
                        "customerEmail", Map.of("type", "string", "description", "The customer's email address")
                ),
                "required", List.of("customerEmail")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String customerEmail = (String) params.get("customerEmail");
        return graphRetriever.getCustomerPurchaseHistory(customerEmail);
    }
}
