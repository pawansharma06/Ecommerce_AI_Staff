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
public class GeminiLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmProvider.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiLlmProvider(
            @Value("${shopai.llm.gemini.api-key:}") String apiKey,
            @Value("${shopai.llm.gemini.model:gemini-1.5-flash}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-1.5-flash";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank() && !apiKey.startsWith("ai-placeholder") && !apiKey.equalsIgnoreCase("none");
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
            throw new IllegalStateException("Google Gemini API Key is not configured.");
        }

        try {
            String endpoint = GEMINI_API_URL + model + ":generateContent?key=" + apiKey;

            Map<String, Object> requestBody = new HashMap<>();

            // System instruction
            if (prompt.systemPrompt() != null && !prompt.systemPrompt().isBlank()) {
                requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", prompt.systemPrompt()))));
            }

            // Contents
            List<Map<String, Object>> contents = new ArrayList<>();
            if (prompt.messages() != null) {
                for (LlmMessage msg : prompt.messages()) {
                    String role = "user".equalsIgnoreCase(msg.role()) ? "user" : "model";
                    contents.add(Map.of("role", role, "parts", List.of(Map.of("text", msg.content() != null ? msg.content() : ""))));
                }
            }
            requestBody.put("contents", contents);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Gemini API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini API error: " + response.statusCode() + " -> " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidate = root.path("candidates").get(0);
            JsonNode textNode = candidate.path("content").path("parts").get(0).path("text");
            String reply = textNode.asText("");

            JsonNode usageNode = root.path("usageMetadata");
            int promptTokens = usageNode.path("promptTokenCount").asInt(50);
            int completionTokens = usageNode.path("candidatesTokenCount").asInt(50);

            return LlmResponse.text(reply, promptTokens, completionTokens, getProviderName(), model);
        } catch (Exception e) {
            log.error("Failed to execute Gemini chat: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini execution failed: " + e.getMessage(), e);
        }
    }
}