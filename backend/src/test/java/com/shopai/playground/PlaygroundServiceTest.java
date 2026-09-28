package com.shopai.playground;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.service.AgentPromptBuilder;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.graph.service.GraphRetriever;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.dto.ToolCall;
import com.shopai.llm.service.LlmService;
import com.shopai.playground.dto.PlaygroundDtos.*;
import com.shopai.playground.service.PlaygroundService;
import com.shopai.rag.service.VectorSearchService;
import com.shopai.tool.core.ToolExecutionEngine;
import com.shopai.tool.core.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaygroundServiceTest {

    @Mock
    private AgentPromptBuilder promptBuilder;
    @Mock
    private CustomerMemoryService customerMemoryService;
    @Mock
    private VectorSearchService vectorSearchService;
    @Mock
    private GraphRetriever graphRetriever;
    @Mock
    private ToolRegistry toolRegistry;
    @Mock
    private ToolExecutionEngine toolExecutionEngine;
    @Mock
    private LlmService llmService;

    private PlaygroundService playgroundService;

    @BeforeEach
    void setUp() {
        playgroundService = new PlaygroundService(
                promptBuilder,
                customerMemoryService,
                vectorSearchService,
                graphRetriever,
                toolRegistry,
                toolExecutionEngine,
                llmService,
                new ObjectMapper()
        );
    }

    @Test
    void shouldExecuteDiagnosticTraceWithTools() {
        when(promptBuilder.buildSystemPrompt(any(), any(), any())).thenReturn("System Prompt Content");
        when(customerMemoryService.getMemoriesByEmail("john@example.com")).thenReturn(Collections.emptyList());
        when(vectorSearchService.search(any(), any(), any(), anyDouble(), anyInt())).thenReturn(Collections.emptyList());
        when(graphRetriever.getCustomerPurchaseHistory("john@example.com")).thenReturn(Collections.emptyList());
        when(toolRegistry.getToolDefinitions()).thenReturn(Collections.emptyList());

        // Step 1: Model requests a tool call
        ToolCall toolCall = new ToolCall("call_1", "search_products", "{\"query\":\"snowboard\"}");
        LlmResponse step1Response = new LlmResponse(
                "Searching for snowboards...",
                List.of(toolCall),
                80, 40, 120, "tool_calls", "openai", "gpt-4o"
        );

        // Step 2: Final response
        LlmResponse step2Response = LlmResponse.text(
                "I found the Top Snowboard in stock for $499.",
                100, 50, "openai", "gpt-4o"
        );

        when(llmService.generateWithTools(any(), any(), any()))
                .thenReturn(step1Response)
                .thenReturn(step2Response);

        when(toolExecutionEngine.executeTool(eq("search_products"), any(), eq("PLAYGROUND"), eq("john@example.com")))
                .thenReturn(Map.of("success", true, "result", Map.of("products", List.of("Top Snowboard")), "status", "COMPLETED", "requiresApproval", false));

        PlaygroundTraceRequest request = new PlaygroundTraceRequest(
                "CUSTOMER_SUPPORT",
                "WEB_CHAT",
                "john@example.com",
                "Show me snowboards",
                0.7,
                5
        );

        PlaygroundTraceResponse trace = playgroundService.executeTrace(request);

        assertThat(trace).isNotNull();
        assertThat(trace.agentType()).isEqualTo("CUSTOMER_SUPPORT");
        assertThat(trace.traceSteps()).hasSize(2);
        assertThat(trace.totalToolsExecuted()).isEqualTo(1);
        assertThat(trace.finalResponse()).contains("Top Snowboard");
        assertThat(trace.injectedContext()).isNotNull();
        assertThat(trace.injectedContext().systemPrompt()).isEqualTo("System Prompt Content");
    }

    @Test
    void shouldPreviewPrompt() {
        when(promptBuilder.buildSystemPrompt("ADMIN_COPILOT", null, "Stock query")).thenReturn("Admin System Prompt with Tools");
        when(vectorSearchService.search(eq("Stock query"), any(), any(), anyDouble(), anyInt())).thenReturn(Collections.emptyList());

        PromptPreviewRequest request = new PromptPreviewRequest("ADMIN_COPILOT", null, "Stock query");
        PromptPreviewResponse preview = playgroundService.previewPrompt(request);

        assertThat(preview).isNotNull();
        assertThat(preview.renderedPrompt()).isEqualTo("Admin System Prompt with Tools");
        assertThat(preview.estimatedTokens()).isGreaterThan(0);
    }
}
