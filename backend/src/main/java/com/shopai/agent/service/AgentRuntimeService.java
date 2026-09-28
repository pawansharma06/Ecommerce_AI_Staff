package com.shopai.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.domain.Conversation;
import com.shopai.agent.domain.ConversationMessage;
import com.shopai.agent.dto.*;
import com.shopai.agent.repository.ConversationMessageRepository;
import com.shopai.agent.repository.ConversationRepository;
import com.shopai.llm.dto.LlmMessage;
import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.dto.ToolCall;
import com.shopai.llm.service.LlmService;
import com.shopai.tool.core.ToolExecutionEngine;
import com.shopai.tool.core.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AgentRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntimeService.class);
    private static final int MAX_AGENT_STEPS = 5;

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final AgentPromptBuilder promptBuilder;
    private final ToolRegistry toolRegistry;
    private final ToolExecutionEngine toolExecutionEngine;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public AgentRuntimeService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            AgentPromptBuilder promptBuilder,
            ToolRegistry toolRegistry,
            ToolExecutionEngine toolExecutionEngine,
            LlmService llmService,
            ObjectMapper objectMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.promptBuilder = promptBuilder;
        this.toolRegistry = toolRegistry;
        this.toolExecutionEngine = toolExecutionEngine;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request, String requestedBy) {
        Conversation conv = new Conversation(
                request.title() != null && !request.title().isBlank() ? request.title() : "New Conversation",
                request.agentType() != null ? request.agentType() : "CUSTOMER_SUPPORT",
                request.channel() != null ? request.channel() : "WEB_CHAT",
                request.customerEmail()
        );
        if (request.customerId() != null) conv.setCustomerId(request.customerId());
        if (request.metadata() != null) {
            try {
                conv.setMetadata(objectMapper.writeValueAsString(request.metadata()));
            } catch (Exception ignored) {}
        }

        conv = conversationRepository.save(conv);
        log.info("Created conversation session {} for agent [{}] by {}", conv.getId(), conv.getAgentType(), requestedBy);
        return ConversationResponse.from(conv, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> listConversations(String agentType, String customerEmail, String search, int page, int size) {
        String cleanType = (agentType != null && !agentType.isBlank() && !"ALL".equalsIgnoreCase(agentType)) ? agentType.trim() : null;
        String cleanEmail = (customerEmail != null && !customerEmail.isBlank()) ? customerEmail.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        return conversationRepository.findWithFilters(cleanType, cleanEmail, cleanSearch, PageRequest.of(page, size))
                .map(ConversationResponse::from);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID id) {
        Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));

        List<ConversationMessage> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        List<MessageResponse> messageResponses = msgs.stream().map(MessageResponse::from).toList();
        return ConversationResponse.from(conv, messageResponses);
    }

    @Transactional
    public void deleteConversation(UUID id) {
        conversationRepository.deleteById(id);
        log.info("Deleted conversation session {}", id);
    }

    @Transactional
    public AgentTurnResult processTurn(UUID conversationId, String userQuery, String customerEmailOverride, String requestedBy) {
        long startTime = System.currentTimeMillis();

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        if (customerEmailOverride != null && !customerEmailOverride.isBlank()) {
            conv.setCustomerEmail(customerEmailOverride.trim().toLowerCase());
        }

        // 1. Save User Message
        ConversationMessage userMsg = ConversationMessage.user(conv, userQuery);
        userMsg = messageRepository.save(userMsg);

        // 2. Load History
        List<ConversationMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        // 3. Build Dynamic System Prompt with Memory & RAG
        String systemPrompt = promptBuilder.buildSystemPrompt(conv.getAgentType(), conv.getCustomerEmail(), userQuery);

        // 4. Convert history to LlmMessages
        List<LlmMessage> llmMessages = new ArrayList<>();
        for (ConversationMessage m : history) {
            if ("user".equalsIgnoreCase(m.getRole())) {
                llmMessages.add(LlmMessage.user(m.getContent()));
            } else if ("assistant".equalsIgnoreCase(m.getRole())) {
                if (m.getToolCalls() != null && !m.getToolCalls().isBlank()) {
                    try {
                        List<ToolCall> calls = objectMapper.readValue(
                                m.getToolCalls(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, ToolCall.class)
                        );
                        llmMessages.add(LlmMessage.assistantWithTools(m.getContent(), calls));
                    } catch (Exception e) {
                        llmMessages.add(LlmMessage.assistant(m.getContent()));
                    }
                } else {
                    llmMessages.add(LlmMessage.assistant(m.getContent()));
                }
            } else if ("tool".equalsIgnoreCase(m.getRole())) {
                llmMessages.add(LlmMessage.toolResult(m.getToolCallId(), m.getToolName(), m.getContent()));
            }
        }

        // 5. Multi-Step Tool Execution Loop
        var toolDefinitions = toolRegistry.getToolDefinitions();
        List<Map<String, Object>> toolsExecuted = new ArrayList<>();
        List<Map<String, Object>> pendingApprovals = new ArrayList<>();
        int totalTokens = 0;
        ConversationMessage finalAssistantMessage = null;

        for (int step = 0; step < MAX_AGENT_STEPS; step++) {
            LlmPrompt prompt = new LlmPrompt(systemPrompt, llmMessages, 0.7, 1024);
            LlmResponse response = llmService.generateWithTools(prompt, toolDefinitions, conv.getAgentType());
            totalTokens += response.totalTokens();

            if (response.hasToolCalls()) {
                // Assistant issued tool call(s)
                String toolCallsJson = "[]";
                try {
                    toolCallsJson = objectMapper.writeValueAsString(response.toolCalls());
                } catch (Exception ignored) {}

                ConversationMessage assistantCallMsg = ConversationMessage.assistantWithTools(conv, response.content(), toolCallsJson);
                assistantCallMsg = messageRepository.save(assistantCallMsg);
                llmMessages.add(LlmMessage.assistantWithTools(response.content(), response.toolCalls()));

                // Execute each tool
                for (ToolCall tc : response.toolCalls()) {
                    Map<String, Object> params = Collections.emptyMap();
                    try {
                        if (tc.argumentsJson() != null && !tc.argumentsJson().isBlank()) {
                            params = objectMapper.readValue(tc.argumentsJson(), Map.class);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse tool call arguments for {}: {}", tc.toolName(), tc.argumentsJson());
                    }

                    Map<String, Object> execResult = toolExecutionEngine.executeTool(
                            tc.toolName(),
                            params,
                            conv.getAgentType(),
                            requestedBy != null ? requestedBy : "user"
                    );

                    toolsExecuted.add(Map.of(
                            "step", step + 1,
                            "toolName", tc.toolName(),
                            "arguments", params,
                            "result", execResult
                    ));

                    if ("PENDING_APPROVAL".equals(execResult.get("status"))) {
                        pendingApprovals.add(execResult);
                    }

                    String resultString = "{}";
                    try {
                        resultString = objectMapper.writeValueAsString(execResult);
                    } catch (Exception ignored) {}

                    ConversationMessage toolMsg = ConversationMessage.toolResult(conv, tc.id(), tc.toolName(), resultString);
                    toolMsg = messageRepository.save(toolMsg);
                    llmMessages.add(LlmMessage.toolResult(tc.id(), tc.toolName(), resultString));
                }
            } else {
                // Final text response reached
                finalAssistantMessage = ConversationMessage.assistant(conv, response.content());
                finalAssistantMessage.setTokenCount(response.totalTokens());
                finalAssistantMessage = messageRepository.save(finalAssistantMessage);
                break;
            }
        }

        if (finalAssistantMessage == null) {
            // Reached max steps fallback
            finalAssistantMessage = ConversationMessage.assistant(
                    conv,
                    "I have processed your request with available store tools. Is there anything else you need assistance with?"
            );
            finalAssistantMessage = messageRepository.save(finalAssistantMessage);
        }

        conv.setUpdatedAt(Instant.now());
        if ("New Conversation".equals(conv.getTitle()) && userQuery.length() > 3) {
            String title = userQuery.length() > 40 ? userQuery.substring(0, 37) + "..." : userQuery;
            conv.setTitle(title);
        }
        conversationRepository.save(conv);

        long latency = System.currentTimeMillis() - startTime;
        log.info("Completed agent turn for conv {} in {}ms (Steps: {}, Tokens: {})",
                conversationId, latency, toolsExecuted.size(), totalTokens);

        return new AgentTurnResult(
                conversationId,
                MessageResponse.from(userMsg),
                MessageResponse.from(finalAssistantMessage),
                toolsExecuted,
                pendingApprovals,
                totalTokens,
                latency
        );
    }
}
