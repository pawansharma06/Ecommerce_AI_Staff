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
public class OpenAiLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmProvider.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiLlmProvider(
            @Value("${shopai.llm.openai.api-key:}") String apiKey,
            @Value("${shopai.llm.openai.model:gpt-4o-mini}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gpt-4o-mini";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank() && !apiKey.startsWith("sk-placeholder") && !apiKey.equalsIgnoreCase("none");
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
            throw new IllegalStateException("OpenAI API Key is not configured.");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", prompt.temperature() != null ? prompt.temperature() : 0.3);
            if (prompt.maxTokens() != null) requestBody.put("max_tokens", prompt.maxTokens());

            List<Map<String, Object>> messagesList = new ArrayList<>();
            if (prompt.systemPrompt() != null && !prompt.systemPrompt().isBlank()) {
                messagesList.add(Map.of("role", "system", "content", prompt.systemPrompt()));
            }

            if (prompt.messages() != null) {
                for (LlmMessage m : prompt.messages()) {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("role", m.role());
                    if (m.content() != null) msgMap.put("content", m.content());
                    if (m.name() != null) msgMap.put("name", m.name());
                    if (m.toolCallId() != null) msgMap.put("tool_call_id", m.toolCallId());
                    messagesList.add(msgMap);
                }
            }
            requestBody.put("messages", messagesList);

            if (tools != null && !tools.isEmpty()) {
                List<Map<String, Object>> toolsList = new ArrayList<>();
                for (ToolDefinition t : tools) {
                    Map<String, Object> func = new HashMap<>();
                    func.put("name", t.name());
                    func.put("description", t.description());
                    func.put("parameters", t.parametersSchema());
                    toolsList.add(Map.of("type", "function", "function", func));
                }
                requestBody.put("tools", toolsList);
            }

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("OpenAI API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("OpenAI API error: " + response.statusCode() + " -> " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choice = root.path("choices").get(0);
            JsonNode messageNode = choice.path("message");
            String content = messageNode.path("content").asText(null);
            String finishReason = choice.path("finish_reason").asText("stop");

            List<ToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = messageNode.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode tcNode : toolCallsNode) {
                    String tcId = tcNode.path("id").asText();
                    String funcName = tcNode.path("function").path("name").asText();
                    String args = tcNode.path("function").path("arguments").asText("{}");
                    toolCalls.add(new ToolCall(tcId, funcName, args));
                }
            }

            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            int totalTokens = usage.path("total_tokens").asInt(promptTokens + completionTokens);

            return new LlmResponse(content, toolCalls, promptTokens, completionTokens, totalTokens, finishReason, getProviderName(), model);
        } catch (Exception e) {
            log.error("Failed to execute OpenAI chat completion: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI execution failed: " + e.getMessage(), e);
        }
    }
}