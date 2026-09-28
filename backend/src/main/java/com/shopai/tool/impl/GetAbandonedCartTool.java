package com.shopai.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.order.domain.AbandonedCheckout;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetAbandonedCartTool implements Tool {

    private final AbandonedCheckoutRepository abandonedCheckoutRepository;
    private final ObjectMapper objectMapper;

    public GetAbandonedCartTool(AbandonedCheckoutRepository abandonedCheckoutRepository, ObjectMapper objectMapper) {
        this.abandonedCheckoutRepository = abandonedCheckoutRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_abandoned_cart";
    }

    @Override
    public String getDescription() {
        return "Retrieve customer abandoned cart details, unpurchased items, total value, and checkout recovery URL.";
    }

    @Override
    public String getRequiredPermission() {
        return "order.read";
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
                        "email", Map.of("type", "string", "description", "Customer email address to lookup abandoned cart"),
                        "token", Map.of("type", "string", "description", "Abandoned cart token (optional)")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String email = (String) params.get("email");
        String token = (String) params.get("token");

        if ((email == null || email.isBlank()) && (token == null || token.isBlank())) {
            return Map.of("error", "Either email or token must be provided");
        }

        String search = (email != null && !email.isBlank()) ? email.trim() : token.trim();
        var page = abandonedCheckoutRepository.findWithFilters(search, null, PageRequest.of(0, 1));

        if (page.isEmpty()) {
            return Map.of("found", false, "message", "No abandoned cart found for the customer");
        }

        AbandonedCheckout checkout = page.getContent().get(0);

        Object parsedItems = Collections.emptyList();
        if (checkout.getLineItems() != null && !checkout.getLineItems().isBlank()) {
            try {
                parsedItems = objectMapper.readValue(checkout.getLineItems(), Object.class);
            } catch (Exception ignored) {}
        }

        return Map.of(
                "found", true,
                "cartToken", checkout.getCartToken() != null ? checkout.getCartToken() : "",
                "customerEmail", checkout.getEmail() != null ? checkout.getEmail() : "",
                "customerName", checkout.getCustomerName() != null ? checkout.getCustomerName() : "",
                "recoveryStatus", checkout.getRecoveryStatus(),
                "totalPrice", checkout.getTotalPrice(),
                "currency", checkout.getCurrency(),
                "recoveryUrl", checkout.getAbandonedCheckoutUrl() != null ? checkout.getAbandonedCheckoutUrl() : "",
                "items", parsedItems,
                "abandonedAt", checkout.getCreatedAt().toString()
        );
    }
}
