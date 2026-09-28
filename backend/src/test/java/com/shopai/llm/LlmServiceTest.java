package com.shopai.llm;

import com.shopai.llm.dto.LlmMessage;
import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.provider.AnthropicLlmProvider;
import com.shopai.llm.provider.GeminiLlmProvider;
import com.shopai.llm.provider.LocalFallbackLlmProvider;
import com.shopai.llm.provider.OpenAiLlmProvider;
import com.shopai.llm.repository.LlmUsageLogRepository;
import com.shopai.llm.service.LlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmServiceTest {

    private OpenAiLlmProvider openAiProvider;
    private GeminiLlmProvider geminiProvider;
    private AnthropicLlmProvider anthropicProvider;
    private LocalFallbackLlmProvider localFallbackProvider;
    private LlmUsageLogRepository usageLogRepository;
    private LlmService llmService;

    @BeforeEach
    void setup() {
        openAiProvider = Mockito.mock(OpenAiLlmProvider.class);
        geminiProvider = Mockito.mock(GeminiLlmProvider.class);
        anthropicProvider = Mockito.mock(AnthropicLlmProvider.class);
        usageLogRepository = Mockito.mock(LlmUsageLogRepository.class);
        localFallbackProvider = new LocalFallbackLlmProvider();
        com.shopai.common.metrics.ShopAiMetricsService metricsService = new com.shopai.common.metrics.ShopAiMetricsService(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        llmService = new LlmService(openAiProvider, geminiProvider, anthropicProvider, localFallbackProvider, usageLogRepository, metricsService);
    }

    @Test
    @DisplayName("LlmService falls back to LocalFallbackLlmProvider when no external provider is configured")
    void testFallbackToLocalProvider() {
        when(openAiProvider.isConfigured()).thenReturn(false);
        when(geminiProvider.isConfigured()).thenReturn(false);
        when(anthropicProvider.isConfigured()).thenReturn(false);

        var activeProvider = llmService.getActiveProvider();
        assertEquals("LOCAL_FALLBACK", activeProvider.getProviderName());

        LlmPrompt prompt = new LlmPrompt("System", List.of(LlmMessage.user("Hello")), 0.7, 100);
        LlmResponse response = llmService.generateChat(prompt, "TestAgent");

        assertNotNull(response);
        assertEquals("LOCAL_FALLBACK", response.providerName());
        assertNotNull(response.content());
        verify(usageLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("LlmService calculates zero cost for local fallback and positive cost for remote")
    void testCostCalculation() {
        BigDecimal localCost = llmService.calculateCost("LOCAL_FALLBACK", "local-sim", 100, 50);
        assertEquals(BigDecimal.ZERO, localCost);

        BigDecimal openAiCost = llmService.calculateCost("OPENAI", "gpt-4o-mini", 1000, 500);
        assertTrue(openAiCost.compareTo(BigDecimal.ZERO) > 0);
    }
}
