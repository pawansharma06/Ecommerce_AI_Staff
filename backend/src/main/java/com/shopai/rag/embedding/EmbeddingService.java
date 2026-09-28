package com.shopai.rag.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String REDIS_EMBEDDING_PREFIX = "shopai:embed:";

    private final OpenAiEmbeddingProvider openAiProvider;
    private final GeminiEmbeddingProvider geminiProvider;
    private final LocalFallbackEmbeddingProvider localFallbackProvider;
    private final StringRedisTemplate redisTemplate;

    public EmbeddingService(
            OpenAiEmbeddingProvider openAiProvider,
            GeminiEmbeddingProvider geminiProvider,
            LocalFallbackEmbeddingProvider localFallbackProvider,
            StringRedisTemplate redisTemplate
    ) {
        this.openAiProvider = openAiProvider;
        this.geminiProvider = geminiProvider;
        this.localFallbackProvider = localFallbackProvider;
        this.redisTemplate = redisTemplate;
    }

    public EmbeddingProvider getActiveProvider() {
        if (openAiProvider.isConfigured()) {
            return openAiProvider;
        }
        if (geminiProvider.isConfigured()) {
            return geminiProvider;
        }
        return localFallbackProvider;
    }

    public String getActiveProviderName() {
        return getActiveProvider().getProviderName();
    }

    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[localFallbackProvider.getDimensions()];
        }

        String cacheKey = REDIS_EMBEDDING_PREFIX + computeHash(text.trim());

        // Check Redis Cache
        try {
            if (redisTemplate != null) {
                String cachedStr = redisTemplate.opsForValue().get(cacheKey);
                if (cachedStr != null && !cachedStr.isBlank()) {
                    return parseVectorString(cachedStr);
                }
            }
        } catch (Exception e) {
            log.debug("Redis embedding cache read bypass: {}", e.getMessage());
        }

        // Generate embedding with active provider
        EmbeddingProvider provider = getActiveProvider();
        float[] vector = provider.generateEmbedding(text);

        // Fallback to local if external provider returned empty
        if (vector == null || vector.length == 0 || isZeroVector(vector)) {
            if (provider != localFallbackProvider) {
                log.info("Falling back to LocalFallbackEmbeddingProvider for text embedding");
                vector = localFallbackProvider.generateEmbedding(text);
            }
        }

        // Cache in Redis for 7 days
        try {
            if (redisTemplate != null && vector != null && vector.length > 0) {
                redisTemplate.opsForValue().set(cacheKey, toVectorString(vector), Duration.ofDays(7));
            }
        } catch (Exception e) {
            log.debug("Redis embedding cache write bypass: {}", e.getMessage());
        }

        return vector;
    }

    public String getEmbeddingAsVectorString(String text) {
        float[] vector = getEmbedding(text);
        return toVectorString(vector);
    }

    public static String toVectorString(float[] vector) {
        if (vector == null || vector.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static float[] parseVectorString(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank() || !vectorStr.startsWith("[") || !vectorStr.endsWith("]")) {
            return new float[0];
        }
        String clean = vectorStr.substring(1, vectorStr.length() - 1);
        if (clean.isBlank()) return new float[0];
        String[] parts = clean.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private boolean isZeroVector(float[] vector) {
        for (float v : vector) {
            if (Math.abs(v) > 1e-6) return false;
        }
        return true;
    }

    private String computeHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}
