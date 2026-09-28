package com.shopai.llm.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.llm.dto.LlmMessage;
import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.dto.ToolCall;
import com.shopai.llm.dto.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LocalFallbackLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalFallbackLlmProvider.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "LOCAL_FALLBACK";
    }

    @Override
    public String getModelName() {
        return "shopai-deterministic-v1";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public LlmResponse generateChat(LlmPrompt prompt) {
        log.debug("Generating response with LocalFallbackLlmProvider");
        String lastUserMessage = extractLastUserMessage(prompt);
        String reply = "I am ShopAI Assistant (Local Simulation Engine). Based on your inquiry: '" + lastUserMessage + "', our store offers guaranteed 30-day returns and real-time shipment tracking.";
        int promptTokens = estimateTokens(prompt.systemPrompt()) + estimateTokens(lastUserMessage);
        int completionTokens = estimateTokens(reply);

        return LlmResponse.text(reply, promptTokens, completionTokens, getProviderName(), getModelName());
    }

    @Override
    public LlmResponse generateWithTools(LlmPrompt prompt, List<ToolDefinition> tools) {
        log.debug("Evaluating tools with LocalFallbackLlmProvider (available tools: {})", tools != null ? tools.size() : 0);
        String lastUserMessage = extractLastUserMessage(prompt).toLowerCase();

        List<ToolCall> toolCalls = new ArrayList<>();

        if (tools != null && !tools.isEmpty()) {
            if (lastUserMessage.contains("order") || lastUserMessage.contains("tracking") || lastUserMessage.contains("status")) {
                findTool(tools, "get_order_status").ifPresent(t -> {
                    toolCalls.add(new ToolCall(UUID.randomUUID().toString(), "get_order_status", "{\"query\":\"#1001\"}"));
                });
            } else if (lastUserMessage.contains("product") || lastUserMessage.contains("jacket") || lastUserMessage.contains("board") || lastUserMessage.contains("search")) {
                findTool(tools, "search_products").ifPresent(t -> {
                    toolCalls.add(new ToolCall(UUID.randomUUID().toString(), "search_products", "{\"query\":\"" + extractKeyword(lastUserMessage) + "\"}"));
                });
            } else if (lastUserMessage.contains("cart") || lastUserMessage.contains("abandon")) {
                findTool(tools, "get_abandoned_cart").ifPresent(t -> {
                    toolCalls.add(new ToolCall(UUID.randomUUID().toString(), "get_abandoned_cart", "{\"email\":\"customer@example.com\"}"));
                });
            } else if (lastUserMessage.contains("policy") || lastUserMessage.contains("return") || lastUserMessage.contains("refund")) {
                findTool(tools, "get_store_policy").ifPresent(t -> {
                    toolCalls.add(new ToolCall(UUID.randomUUID().toString(), "get_store_policy", "{\"topic\":\"returns\"}"));
                });
            }
        }

        int promptTokens = estimateTokens(prompt.systemPrompt()) + estimateTokens(lastUserMessage);
        int completionTokens = toolCalls.isEmpty() ? 50 : 25;

        if (!toolCalls.isEmpty()) {
            return new LlmResponse(null, toolCalls, promptTokens, completionTokens, promptTokens + completionTokens, "tool_calls", getProviderName(), getModelName());
        }

        return generateChat(prompt);
    }

    private Optional<ToolDefinition> findTool(List<ToolDefinition> tools, String name) {
        return tools.stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst();
    }

    private String extractLastUserMessage(LlmPrompt prompt) {
        if (prompt.messages() == null || prompt.messages().isEmpty()) return "";
        for (int i = prompt.messages().size() - 1; i >= 0; i--) {
            LlmMessage msg = prompt.messages().get(i);
            if ("user".equalsIgnoreCase(msg.role()) && msg.content() != null) {
                return msg.content();
            }
        }
        return "";
    }

    private String extractKeyword(String query) {
        String clean = query.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        return clean.isEmpty() ? "catalog" : clean;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return Math.max(1, text.length() / 4);
    }
}