package com.shopai.rag.embedding;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component
public class LocalFallbackEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSIONS = 1536;

    @Override
    public String getProviderName() {
        return "LOCAL_FALLBACK";
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIMENSIONS];
        }

        float[] vector = new float[DIMENSIONS];
        String clean = text.toLowerCase().trim();
        String[] words = clean.split("\\W+");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String word : words) {
                if (word.isBlank()) continue;
                byte[] hash = md.digest(word.getBytes(StandardCharsets.UTF_8));
                for (int i = 0; i < hash.length; i++) {
                    int idx = (Math.abs(hash[i] * 31 + i * 17)) % DIMENSIONS;
                    vector[idx] += (float) (hash[i] / 128.0);
                }
            }

            // Global text hash seed
            byte[] fullHash = md.digest(clean.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < fullHash.length; i++) {
                int idx = (Math.abs(fullHash[i] * 47 + i * 13)) % DIMENSIONS;
                vector[idx] += (float) (fullHash[i] / 64.0);
            }

            // Normalize vector to unit length (L2 norm)
            double sumSq = 0.0;
            for (float v : vector) {
                sumSq += v * v;
            }
            if (sumSq > 0) {
                float norm = (float) Math.sqrt(sumSq);
                for (int i = 0; i < DIMENSIONS; i++) {
                    vector[i] /= norm;
                }
            }
        } catch (Exception e) {
            // fallback zero vector
        }

        return vector;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(generateEmbedding(text));
        }
        return results;
    }
}
