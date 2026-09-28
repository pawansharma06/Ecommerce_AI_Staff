package com.shopai.rag.embedding;

import java.util.List;

public interface EmbeddingProvider {

    String getProviderName();

    int getDimensions();

    float[] generateEmbedding(String text);

    List<float[]> generateEmbeddings(List<String> texts);
}
