package com.shopai.agent.service;

import com.shopai.customer.domain.CustomerMemory;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.rag.service.VectorSearchService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentPromptBuilder {

    private final CustomerMemoryService customerMemoryService;
    private final VectorSearchService vectorSearchService;

    public AgentPromptBuilder(CustomerMemoryService customerMemoryService, VectorSearchService vectorSearchService) {
        this.customerMemoryService = customerMemoryService;
        this.vectorSearchService = vectorSearchService;
    }

    public String buildSystemPrompt(String agentType, String customerEmail, String userQuery) {
        StringBuilder sb = new StringBuilder();

        // 1. Core Persona
        if ("ADMIN_COPILOT".equalsIgnoreCase(agentType)) {
            sb.append("""
                    You are ShopAI Merchant Admin Copilot, an autonomous operational assistant for the store owner.
                    Your responsibilities:
                    - Analyze sales, unfulfilled orders, inventory stock levels, and abandoned checkouts.
                    - Assist the merchant with stock adjustments and order refund operations using tools.
                    - Always cite specific order numbers, SKU IDs, variant titles, and revenue metrics.
                    """);
        } else {
            sb.append("""
                    You are ShopAI Customer Shopping & Support Assistant, an empathetic, helpful, and highly knowledgeable e-commerce concierge.
                    Your responsibilities:
                    - Help customers find products matching their style, preferences, and size requirements.
                    - Provide real-time order status, fulfillment carrier tracking URLs, and delivery estimates.
                    - Answer store policy questions regarding returns, exchanges, refunds, and shipping rules.
                    - Help recover abandoned carts by providing direct checkout recovery links.
                    """);
        }

        // 2. Strict Safety Guardrails (Rules 9, 10, 14, 15, 16, 27, 29, 38)
        sb.append("""
                
                CRITICAL OPERATIONAL RULES:
                1. Never invent fake Shopify data, orders, tracking links, or pricing. Always query via tools.
                2. Never disclose API keys, private tokens, internal credentials, or execute arbitrary code/SQL.
                3. High-risk operations (such as refunds) require explicit merchant approval. If a tool returns PENDING_APPROVAL, inform the user/merchant that the action has been safely queued for approval.
                4. Maintain customer data privacy and store isolation at all times.
                """);

        // 3. Dynamic Customer Memory Context Injection
        if (customerEmail != null && !customerEmail.isBlank()) {
            List<CustomerMemory> memories = customerMemoryService.getMemoriesByEmail(customerEmail.trim());
            if (!memories.isEmpty()) {
                sb.append("\nCUSTOMER CONTEXT & PREFERENCES FOR [").append(customerEmail.trim()).append("]:\n");
                for (CustomerMemory mem : memories) {
                    sb.append("- ").append(mem.getCategory()).append(" [").append(mem.getMemoryKey()).append("]: ")
                            .append(mem.getMemoryValue()).append("\n");
                }
            }
        }

        // 4. Dynamic Vector RAG Knowledge Context Injection
        if (userQuery != null && !userQuery.isBlank()) {
            List<VectorSearchService.SearchResultItem> knowledge = vectorSearchService.search(userQuery, null, null, 0.35, 3);
            if (!knowledge.isEmpty()) {
                sb.append("\nRELEVANT STORE KNOWLEDGE BASE CONTEXT:\n");
                for (VectorSearchService.SearchResultItem item : knowledge) {
                    sb.append("--- [").append(item.documentType()).append("] ").append(item.documentTitle()).append(" ---\n")
                            .append(item.content()).append("\n");
                }
            }
        }

        return sb.toString();
    }
}
