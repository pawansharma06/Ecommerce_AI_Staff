package com.shopai.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingProvider.class);
    private static final int DIMENSIONS = 1536;

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingProvider(
            @Value("${shopai.ai.gemini.api-key:}") String apiKey,
            @Value("${shopai.ai.gemini.embedding-model:text-embedding-004}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (!isConfigured() || text == null || text.isBlank()) {
            return new float[DIMENSIONS];
        }

        try {
            Map<String, Object> textPart = Map.of("text", text);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> body = Map.of(
                    "model", "models/" + model,
                    "content", content,
                    "outputDimensionality", DIMENSIONS
            );

            String requestJson = objectMapper.writeValueAsString(body);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":embedContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Gemini embedding API returned status {}: {}", response.statusCode(), response.body());
                return new float[DIMENSIONS];
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode values = root.path("embedding").path("values");

            if (values.isArray()) {
                float[] vec = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    vec[i] = (float) values.get(i).asDouble();
                }
                return vec;
            }

            return new float[DIMENSIONS];
        } catch (Exception e) {
            log.error("Failed to generate Gemini embedding: {}", e.getMessage());
            return new float[DIMENSIONS];
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> list = new ArrayList<>();
        for (String t : texts) {
            list.add(generateEmbedding(t));
        }
        return list;
    }
}
