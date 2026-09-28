package com.shopai.tool.impl;

import com.shopai.customer.domain.CustomerMemory;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetCustomerMemoryTool implements Tool {

    private final CustomerMemoryService customerMemoryService;

    public GetCustomerMemoryTool(CustomerMemoryService customerMemoryService) {
        this.customerMemoryService = customerMemoryService;
    }

    @Override
    public String getName() {
        return "get_customer_memory";
    }

    @Override
    public String getDescription() {
        return "Retrieve customer preferences, past sizes, style notes, and profile memories by customer email.";
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
                        "email", Map.of("type", "string", "description", "Customer email address"),
                        "query", Map.of("type", "string", "description", "Specific preference query or semantic topic to search in memory (optional)")
                ),
                "required", List.of("email")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String email = (String) params.get("email");
        if (email == null || email.isBlank()) {
            return Map.of("error", "Email is required");
        }

        String query = (String) params.get("query");
        if (query != null && !query.isBlank()) {
            List<Map<String, Object>> searchResults = customerMemoryService.searchCustomerMemories(email.trim(), query.trim(), 0.20, 5);
            return Map.of(
                    "customerEmail", email.trim(),
                    "memories", searchResults
            );
        }

        List<CustomerMemory> memories = customerMemoryService.getMemoriesByEmail(email.trim());
        List<Map<String, Object>> list = memories.stream().map(m -> Map.<String, Object>of(
                "category", m.getCategory(),
                "key", m.getMemoryKey(),
                "value", m.getMemoryValue(),
                "confidence", m.getConfidenceScore(),
                "updatedAt", m.getUpdatedAt().toString()
        )).toList();

        return Map.of(
                "customerEmail", email.trim(),
                "memories", list
        );
    }
}
