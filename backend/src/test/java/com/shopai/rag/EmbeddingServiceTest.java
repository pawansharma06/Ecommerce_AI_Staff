package com.shopai.rag;

import com.shopai.rag.embedding.GeminiEmbeddingProvider;
import com.shopai.rag.embedding.LocalFallbackEmbeddingProvider;
import com.shopai.rag.embedding.OpenAiEmbeddingProvider;
import com.shopai.rag.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmbeddingServiceTest {

    @Mock
    private OpenAiEmbeddingProvider openAiProvider;

    @Mock
    private GeminiEmbeddingProvider geminiProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LocalFallbackEmbeddingProvider localFallbackProvider;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        localFallbackProvider = new LocalFallbackEmbeddingProvider();
        when(openAiProvider.isConfigured()).thenReturn(false);
        when(geminiProvider.isConfigured()).thenReturn(false);
        embeddingService = new EmbeddingService(openAiProvider, geminiProvider, localFallbackProvider, redisTemplate);
    }

    @Test
    void testGenerateEmbeddingDimensionAndFormat() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        float[] embedding = embeddingService.getEmbedding("High quality winter jacket");
        assertThat(embedding).isNotNull();
        assertThat(embedding).hasSize(1536);

        String vectorStr = embeddingService.getEmbeddingAsVectorString("High quality winter jacket");
        assertThat(vectorStr).startsWith("[").endsWith("]");
        assertThat(vectorStr.split(",")).hasSize(1536);
    }

    @Test
    void testEmbeddingCacheHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("[0.1,0.2,0.3]");

        String vectorStr = embeddingService.getEmbeddingAsVectorString("Cached query");
        assertThat(vectorStr).isEqualTo("[0.1,0.2,0.3]");
        verify(valueOperations, never()).set(anyString(), anyString(), any());
    }

    @Test
    void testActiveProviderFallback() {
        assertThat(embeddingService.getActiveProviderName()).isEqualTo("LOCAL_FALLBACK");
    }
}