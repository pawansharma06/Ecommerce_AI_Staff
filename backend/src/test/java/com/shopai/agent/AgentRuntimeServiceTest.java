package com.shopai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.domain.Conversation;
import com.shopai.agent.domain.ConversationMessage;
import com.shopai.agent.dto.AgentTurnResult;
import com.shopai.agent.dto.CreateConversationRequest;
import com.shopai.agent.dto.ConversationResponse;
import com.shopai.agent.repository.ConversationMessageRepository;
import com.shopai.agent.repository.ConversationRepository;
import com.shopai.agent.service.AgentPromptBuilder;
import com.shopai.agent.service.AgentRuntimeService;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.dto.ToolCall;
import com.shopai.llm.service.LlmService;
import com.shopai.tool.core.ToolExecutionEngine;
import com.shopai.tool.core.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AgentRuntimeServiceTest {

    private ConversationRepository conversationRepository;
    private ConversationMessageRepository messageRepository;
    private AgentPromptBuilder promptBuilder;
    private ToolRegistry toolRegistry;
    private ToolExecutionEngine toolExecutionEngine;
    private LlmService llmService;
    private ObjectMapper objectMapper;
    private AgentRuntimeService runtimeService;

    @BeforeEach
    void setup() {
        conversationRepository = Mockito.mock(ConversationRepository.class);
        messageRepository = Mockito.mock(ConversationMessageRepository.class);
        promptBuilder = Mockito.mock(AgentPromptBuilder.class);
        toolRegistry = Mockito.mock(ToolRegistry.class);
        toolExecutionEngine = Mockito.mock(ToolExecutionEngine.class);
        llmService = Mockito.mock(LlmService.class);
        objectMapper = new ObjectMapper();

        runtimeService = new AgentRuntimeService(
                conversationRepository,
                messageRepository,
                promptBuilder,
                toolRegistry,
                toolExecutionEngine,
                llmService,
                objectMapper
        );
    }

    @Test
    @DisplayName("createConversation creates and persists a new session")
    void testCreateConversation() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> {
            Conversation c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CreateConversationRequest req = new CreateConversationRequest(
                "Support Session", "CUSTOMER_SUPPORT", "WEB_CHAT", "customer@example.com", 123L, Map.of("test", true)
        );

        ConversationResponse resp = runtimeService.createConversation(req, "admin");

        assertNotNull(resp);
        assertEquals("Support Session", resp.title());
        assertEquals("CUSTOMER_SUPPORT", resp.agentType());
        assertEquals("customer@example.com", resp.customerEmail());
        verify(conversationRepository, times(1)).save(any(Conversation.class));
    }

    @Test
    @DisplayName("processTurn executes tool calling loop and saves messages")
    void testProcessTurnWithTools() {
        UUID convId = UUID.randomUUID();
        Conversation conv = new Conversation("Test Chat", "CUSTOMER_SUPPORT", "WEB_CHAT", "test@example.com");
        conv.setId(convId);

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(promptBuilder.buildSystemPrompt(any(), any(), any())).thenReturn("System Prompt");
        when(toolRegistry.getToolDefinitions()).thenReturn(List.of());
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(i -> {
            ConversationMessage m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        // Step 1: LLM issues a tool call (search_products)
        ToolCall call = new ToolCall("call_1", "search_products", "{\"query\":\"boots\"}");
        LlmResponse resp1 = new LlmResponse(
                "Let me search our catalog for boots.",
                List.of(call),
                50, 20, 70, "tool_calls",
                "LOCAL_FALLBACK",
                "sim"
        );

        // Step 2: LLM provides final answer
        LlmResponse resp2 = new LlmResponse(
                "We found Snow Boots in size 10 in stock.",
                List.of(),
                80, 40, 120, "stop",
                "LOCAL_FALLBACK",
                "sim"
        );

        when(llmService.generateWithTools(any(), any(), any()))
                .thenReturn(resp1)
                .thenReturn(resp2);

        when(toolExecutionEngine.executeTool(eq("search_products"), any(), any(), any()))
                .thenReturn(Map.of("status", "SUCCESS", "items", List.of("Snow Boots")));

        AgentTurnResult result = runtimeService.processTurn(convId, "Find me winter boots", null, "operator");

        assertNotNull(result);
        assertEquals(convId, result.conversationId());
        assertEquals("Find me winter boots", result.userMessage().content());
        assertEquals("We found Snow Boots in size 10 in stock.", result.assistantMessage().content());
        assertEquals(1, result.toolsExecuted().size());
        assertEquals("search_products", result.toolsExecuted().get(0).get("toolName"));
        verify(toolExecutionEngine, times(1)).executeTool(eq("search_products"), any(), any(), any());
    }
}
