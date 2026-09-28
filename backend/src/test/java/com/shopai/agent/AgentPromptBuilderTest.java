package com.shopai.agent;

import com.shopai.agent.service.AgentPromptBuilder;
import com.shopai.customer.domain.CustomerMemory;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.rag.service.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentPromptBuilderTest {

    private CustomerMemoryService customerMemoryService;
    private VectorSearchService vectorSearchService;
    private AgentPromptBuilder promptBuilder;

    @BeforeEach
    void setup() {
        customerMemoryService = Mockito.mock(CustomerMemoryService.class);
        vectorSearchService = Mockito.mock(VectorSearchService.class);
        promptBuilder = new AgentPromptBuilder(customerMemoryService, vectorSearchService);
    }

    @Test
    @DisplayName("Prompt builder injects Admin Copilot persona and safety rules")
    void testAdminCopilotPersona() {
        when(vectorSearchService.search(any(), any(), any(), anyDouble(), anyInt())).thenReturn(List.of());

        String prompt = promptBuilder.buildSystemPrompt("ADMIN_COPILOT", null, "check sales");

        assertNotNull(prompt);
        assertTrue(prompt.contains("Merchant Admin Copilot"));
        assertTrue(prompt.contains("CRITICAL OPERATIONAL RULES"));
    }

    @Test
    @DisplayName("Prompt builder injects Customer Memory facts when customerEmail is present")
    void testCustomerMemoryInjection() {
        CustomerMemory mem = new CustomerMemory("jane@example.com", "PREFERENCE", "board_size", "156cm");
        mem.setConfidenceScore(BigDecimal.valueOf(0.95));

        when(customerMemoryService.getMemoriesByEmail("jane@example.com")).thenReturn(List.of(mem));
        when(vectorSearchService.search(any(), any(), any(), anyDouble(), anyInt())).thenReturn(List.of());

        String prompt = promptBuilder.buildSystemPrompt("CUSTOMER_SUPPORT", "jane@example.com", "find board");

        assertNotNull(prompt);
        assertTrue(prompt.contains("CUSTOMER CONTEXT & PREFERENCES FOR [jane@example.com]"));
        assertTrue(prompt.contains("board_size"));
        assertTrue(prompt.contains("156cm"));
    }

    @Test
    @DisplayName("Prompt builder injects RAG Knowledge snippets when query matches")
    void testRagKnowledgeInjection() {
        VectorSearchService.SearchResultItem item = new VectorSearchService.SearchResultItem(
                "chunk-1",
                "doc-1",
                "Store Return Policy",
                "POLICY",
                "Items can be returned within 30 days of delivery.",
                50,
                null,
                0.88,
                Map.of()
        );

        when(vectorSearchService.search(eq("returns"), isNull(), isNull(), anyDouble(), eq(3)))
                .thenReturn(List.of(item));

        String prompt = promptBuilder.buildSystemPrompt("CUSTOMER_SUPPORT", null, "returns");

        assertNotNull(prompt);
        assertTrue(prompt.contains("RELEVANT STORE KNOWLEDGE BASE CONTEXT"));
        assertTrue(prompt.contains("Store Return Policy"));
        assertTrue(prompt.contains("Items can be returned within 30 days of delivery."));
    }
}
