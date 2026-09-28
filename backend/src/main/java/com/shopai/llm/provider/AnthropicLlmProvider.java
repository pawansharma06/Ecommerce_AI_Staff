package com.shopai.llm.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.llm.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
public class AnthropicLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmProvider.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicLlmProvider(
            @Value("${shopai.llm.anthropic.api-key:}") String apiKey,
            @Value("${shopai.llm.anthropic.model:claude-3-5-sonnet-20241022}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "claude-3-5-sonnet-20241022";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getProviderName() {
        return "ANTHROPIC";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank() && !apiKey.startsWith("sk-ant-placeholder") && !apiKey.equalsIgnoreCase("none");
    }

    @Override
    public LlmResponse generateChat(LlmPrompt prompt) {
        return executeCompletion(prompt, null);
    }

    @Override
    public LlmResponse generateWithTools(LlmPrompt prompt, List<ToolDefinition> tools) {
        return executeCompletion(prompt, tools);
    }

    private LlmResponse executeCompletion(LlmPrompt prompt, List<ToolDefinition> tools) {
        if (!isConfigured()) {
            throw new IllegalStateException("Anthropic API Key is not configured.");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", prompt.maxTokens() != null ? prompt.maxTokens() : 1024);
            if (prompt.systemPrompt() != null && !prompt.systemPrompt().isBlank()) {
                requestBody.put("system", prompt.systemPrompt());
            }

            List<Map<String, Object>> messagesList = new ArrayList<>();
            if (prompt.messages() != null) {
                for (LlmMessage m : prompt.messages()) {
                    messagesList.add(Map.of("role", m.role(), "content", m.content() != null ? m.content() : ""));
                }
            }
            requestBody.put("messages", messagesList);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_API_URL))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Anthropic API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Anthropic API error: " + response.statusCode() + " -> " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("content").get(0).path("text").asText("");
            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("input_tokens").asInt(0);
            int completionTokens = usage.path("output_tokens").asInt(0);

            return LlmResponse.text(text, promptTokens, completionTokens, getProviderName(), model);
        } catch (Exception e) {
            log.error("Failed to execute Anthropic completion: {}", e.getMessage(), e);
            throw new RuntimeException("Anthropic execution failed: " + e.getMessage(), e);
        }
    }
}