package com.shopai.playground.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.service.AgentPromptBuilder;
import com.shopai.customer.domain.CustomerMemory;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.graph.service.GraphRetriever;
import com.shopai.llm.dto.LlmMessage;
import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.dto.ToolCall;
import com.shopai.llm.service.LlmService;
import com.shopai.playground.dto.PlaygroundDtos.*;
import com.shopai.rag.service.VectorSearchService;
import com.shopai.rag.service.VectorSearchService.SearchResultItem;
import com.shopai.tool.core.ToolExecutionEngine;
import com.shopai.tool.core.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlaygroundService {

    private static final Logger log = LoggerFactory.getLogger(PlaygroundService.class);

    private final AgentPromptBuilder promptBuilder;
    private final CustomerMemoryService customerMemoryService;
    private final VectorSearchService vectorSearchService;
    private final GraphRetriever graphRetriever;
    private final ToolRegistry toolRegistry;
    private final ToolExecutionEngine toolExecutionEngine;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public PlaygroundService(
            AgentPromptBuilder promptBuilder,
            CustomerMemoryService customerMemoryService,
            VectorSearchService vectorSearchService,
            GraphRetriever graphRetriever,
            ToolRegistry toolRegistry,
            ToolExecutionEngine toolExecutionEngine,
            LlmService llmService,
            ObjectMapper objectMapper
    ) {
        this.promptBuilder = promptBuilder;
        this.customerMemoryService = customerMemoryService;
        this.vectorSearchService = vectorSearchService;
        this.graphRetriever = graphRetriever;
        this.toolRegistry = toolRegistry;
        this.toolExecutionEngine = toolExecutionEngine;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public PlaygroundTraceResponse executeTrace(PlaygroundTraceRequest request) {
        long overallStart = System.currentTimeMillis();

        String agentType = request.agentType() != null && !request.agentType().isBlank() ? request.agentType() : "CUSTOMER_SUPPORT";
        String channel = request.channel() != null && !request.channel().isBlank() ? request.channel() : "WEB_CHAT";
        String userQuery = request.userQuery() != null ? request.userQuery() : "Hello";
        String customerEmail = request.customerEmail() != null && !request.customerEmail().isBlank() ? request.customerEmail().trim() : null;
        double temperature = request.temperature() != null ? request.temperature() : 0.7;
        int maxSteps = request.maxSteps() != null && request.maxSteps() > 0 ? Math.min(request.maxSteps(), 10) : 5;

        // 1. Injected Context Extraction
        String systemPrompt = promptBuilder.buildSystemPrompt(agentType, customerEmail, userQuery);

        List<InjectedMemoryItem> memoryItems = new ArrayList<>();
        if (customerEmail != null) {
            List<CustomerMemory> memories = customerMemoryService.getMemoriesByEmail(customerEmail);
            for (CustomerMemory mem : memories) {
                memoryItems.add(new InjectedMemoryItem(mem.getCategory(), mem.getMemoryKey(), mem.getMemoryValue()));
            }
        }

        List<InjectedRagChunk> ragChunks = new ArrayList<>();
        try {
            List<SearchResultItem> searchResults = vectorSearchService.search(userQuery, customerEmail, null, 0.4, 3);
            for (SearchResultItem sr : searchResults) {
                String snippet = sr.content() != null && sr.content().length() > 160 ? sr.content().substring(0, 160) + "..." : sr.content();
                ragChunks.add(new InjectedRagChunk(sr.documentTitle(), snippet, sr.similarityScore()));
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> graphRelationships = new ArrayList<>();
        try {
            if (customerEmail != null) {
                var purchases = graphRetriever.getCustomerPurchaseHistory(customerEmail);
                for (var p : purchases) {
                    graphRelationships.add(Map.of("type", "PURCHASED", "product", p.getOrDefault("productTitle", "Product")));
                }
            }
        } catch (Exception ignored) {}

        InjectedContextSummary contextSummary = new InjectedContextSummary(
                systemPrompt,
                memoryItems,
                ragChunks,
                graphRelationships
        );

        // 2. Multi-Step Sandboxed Reasoning Loop
        List<LlmMessage> llmMessages = new ArrayList<>();
        llmMessages.add(LlmMessage.user(userQuery));

        var toolDefinitions = toolRegistry.getToolDefinitions();
        List<TraceStepItem> traceSteps = new ArrayList<>();
        int totalTokens = 0;
        int totalToolsExecuted = 0;
        String finalResponse = "";

        for (int step = 0; step < maxSteps; step++) {
            long stepStart = System.currentTimeMillis();
            LlmPrompt prompt = new LlmPrompt(systemPrompt, llmMessages, temperature, 1024);
            LlmResponse response = llmService.generateWithTools(prompt, toolDefinitions, agentType);

            long stepLatency = System.currentTimeMillis() - stepStart;
            totalTokens += response.totalTokens();

            if (response.hasToolCalls()) {
                List<ToolTraceItem> stepTools = new ArrayList<>();

                llmMessages.add(LlmMessage.assistantWithTools(response.content(), response.toolCalls()));

                for (ToolCall call : response.toolCalls()) {
                    long toolStart = System.currentTimeMillis();
                    Map<String, Object> parsedArgs = parseToolArgs(call.argumentsJson());
                    Map<String, Object> toolResultMap = toolExecutionEngine.executeTool(
                            call.toolName(),
                            parsedArgs,
                            "PLAYGROUND",
                            customerEmail
                    );
                    long toolLatency = System.currentTimeMillis() - toolStart;
                    totalToolsExecuted++;

                    boolean reqApproval = Boolean.TRUE.equals(toolResultMap.get("requiresApproval"));
                    String status = (String) toolResultMap.getOrDefault("status", "COMPLETED");

                    String outputString;
                    try {
                        outputString = objectMapper.writeValueAsString(toolResultMap.getOrDefault("result", toolResultMap));
                    } catch (Exception e) {
                        outputString = String.valueOf(toolResultMap);
                    }

                    stepTools.add(new ToolTraceItem(
                            call.id(),
                            call.toolName(),
                            parsedArgs,
                            outputString,
                            reqApproval,
                            status,
                            toolLatency
                    ));

                    llmMessages.add(LlmMessage.toolResult(call.id(), call.toolName(), outputString));
                }

                traceSteps.add(new TraceStepItem(
                        step + 1,
                        response.content(),
                        stepTools,
                        stepLatency,
                        response.totalTokens()
                ));
            } else {
                // Final answer produced
                finalResponse = response.content();
                traceSteps.add(new TraceStepItem(
                        step + 1,
                        finalResponse,
                        Collections.emptyList(),
                        stepLatency,
                        response.totalTokens()
                ));
                break;
            }
        }

        long totalLatency = System.currentTimeMillis() - overallStart;

        return new PlaygroundTraceResponse(
                agentType,
                channel,
                userQuery,
                contextSummary,
                traceSteps,
                finalResponse,
                totalTokens,
                totalToolsExecuted,
                totalLatency
        );
    }

    public PromptPreviewResponse previewPrompt(PromptPreviewRequest request) {
        String agentType = request.agentType() != null && !request.agentType().isBlank() ? request.agentType() : "CUSTOMER_SUPPORT";
        String customerEmail = request.customerEmail() != null && !request.customerEmail().isBlank() ? request.customerEmail().trim() : null;
        String userQuery = request.userQuery() != null ? request.userQuery() : "Product inquiry";

        String renderedPrompt = promptBuilder.buildSystemPrompt(agentType, customerEmail, userQuery);
        int estTokens = (int) Math.ceil(renderedPrompt.length() / 4.0);

        List<InjectedMemoryItem> memoryItems = new ArrayList<>();
        if (customerEmail != null) {
            List<CustomerMemory> memories = customerMemoryService.getMemoriesByEmail(customerEmail);
            for (CustomerMemory mem : memories) {
                memoryItems.add(new InjectedMemoryItem(mem.getCategory(), mem.getMemoryKey(), mem.getMemoryValue()));
            }
        }

        List<InjectedRagChunk> ragChunks = new ArrayList<>();
        try {
            List<SearchResultItem> searchResults = vectorSearchService.search(userQuery, customerEmail, null, 0.4, 3);
            for (SearchResultItem sr : searchResults) {
                String snippet = sr.content() != null && sr.content().length() > 160 ? sr.content().substring(0, 160) + "..." : sr.content();
                ragChunks.add(new InjectedRagChunk(sr.documentTitle(), snippet, sr.similarityScore()));
            }
        } catch (Exception ignored) {}

        return new PromptPreviewResponse(
                agentType,
                customerEmail,
                renderedPrompt,
                estTokens,
                memoryItems,
                ragChunks
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseToolArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(argsJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
