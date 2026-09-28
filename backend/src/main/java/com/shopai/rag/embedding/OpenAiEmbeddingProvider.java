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
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingProvider.class);
    private static final int DIMENSIONS = 1536;

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingProvider(
            @Value("${shopai.ai.openai.api-key:}") String apiKey,
            @Value("${shopai.ai.openai.embedding-model:text-embedding-3-small}") String model,
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
        return "OPENAI";
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public float[] generateEmbedding(String text) {
        List<float[]> results = generateEmbeddings(List.of(text));
        return results.isEmpty() ? new float[DIMENSIONS] : results.get(0);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (!isConfigured() || texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("input", texts);
            body.put("model", model);
            body.put("dimensions", DIMENSIONS);

            String requestJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("OpenAI embedding API returned status {}: {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode dataArray = root.path("data");
            List<float[]> embeddings = new ArrayList<>();

            if (dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    JsonNode embNode = item.path("embedding");
                    if (embNode.isArray()) {
                        float[] vec = new float[embNode.size()];
                        for (int i = 0; i < embNode.size(); i++) {
                            vec[i] = (float) embNode.get(i).asDouble();
                        }
                        embeddings.add(vec);
                    }
                }
            }

            return embeddings;
        } catch (Exception e) {
            log.error("Failed to generate OpenAI embeddings: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
