package com.shopai.llm.service;

import com.shopai.llm.domain.LlmUsageLog;
import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.dto.ToolDefinition;
import com.shopai.llm.provider.*;
import com.shopai.llm.repository.LlmUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final OpenAiLlmProvider openAiProvider;
    private final GeminiLlmProvider geminiProvider;
    private final AnthropicLlmProvider anthropicProvider;
    private final LocalFallbackLlmProvider localFallbackProvider;
    private final LlmUsageLogRepository usageLogRepository;
    private final com.shopai.common.metrics.ShopAiMetricsService metricsService;

    public LlmService(
            OpenAiLlmProvider openAiProvider,
            GeminiLlmProvider geminiProvider,
            AnthropicLlmProvider anthropicProvider,
            LocalFallbackLlmProvider localFallbackProvider,
            LlmUsageLogRepository usageLogRepository,
            com.shopai.common.metrics.ShopAiMetricsService metricsService
    ) {
        this.openAiProvider = openAiProvider;
        this.geminiProvider = geminiProvider;
        this.anthropicProvider = anthropicProvider;
        this.localFallbackProvider = localFallbackProvider;
        this.usageLogRepository = usageLogRepository;
        this.metricsService = metricsService;
    }

    public LlmProvider getActiveProvider() {
        if (openAiProvider.isConfigured()) return openAiProvider;
        if (geminiProvider.isConfigured()) return geminiProvider;
        if (anthropicProvider.isConfigured()) return anthropicProvider;
        return localFallbackProvider;
    }

    @Transactional
    public LlmResponse generateChat(LlmPrompt prompt, String agentName) {
        LlmProvider provider = getActiveProvider();
        long start = System.currentTimeMillis();
        try {
            LlmResponse response = provider.generateChat(prompt);
            long latencyMs = System.currentTimeMillis() - start;
            recordUsage(response, latencyMs, agentName);
            return response;
        } catch (Exception e) {
            log.warn("Active provider {} failed: {}. Falling back to Local simulation", provider.getProviderName(), e.getMessage());
            long latencyMs = System.currentTimeMillis() - start;
            LlmResponse fallbackResp = localFallbackProvider.generateChat(prompt);
            recordUsage(fallbackResp, latencyMs, agentName);
            return fallbackResp;
        }
    }

    @Transactional
    public LlmResponse generateWithTools(LlmPrompt prompt, List<ToolDefinition> tools, String agentName) {
        LlmProvider provider = getActiveProvider();
        long start = System.currentTimeMillis();
        try {
            LlmResponse response = provider.generateWithTools(prompt, tools);
            long latencyMs = System.currentTimeMillis() - start;
            recordUsage(response, latencyMs, agentName);
            return response;
        } catch (Exception e) {
            log.warn("Active provider {} tool generation failed: {}. Falling back to Local simulation", provider.getProviderName(), e.getMessage());
            long latencyMs = System.currentTimeMillis() - start;
            LlmResponse fallbackResp = localFallbackProvider.generateWithTools(prompt, tools);
            recordUsage(fallbackResp, latencyMs, agentName);
            return fallbackResp;
        }
    }

    private void recordUsage(LlmResponse response, long latencyMs, String agentName) {
        if (response == null) return;
        BigDecimal cost = calculateCost(response.providerName(), response.modelName(), response.promptTokens(), response.completionTokens());
        try {
            LlmUsageLog usage = new LlmUsageLog(
                    response.providerName(),
                    response.modelName(),
                    agentName != null ? agentName : "SYSTEM",
                    response.promptTokens(),
                    response.completionTokens(),
                    latencyMs,
                    cost
            );
            usageLogRepository.save(usage);
            if (metricsService != null) {
                metricsService.recordLlmRequest(response.providerName(), response.modelName(), agentName, true, latencyMs, response.promptTokens(), response.completionTokens());
            }
        } catch (Exception e) {
            log.error("Failed to record LLM usage log: {}", e.getMessage());
        }
    }

    public BigDecimal calculateCost(String provider, String model, int promptTokens, int completionTokens) {
        if ("LOCAL_FALLBACK".equalsIgnoreCase(provider)) {
            return BigDecimal.ZERO;
        }
        // Blended rate estimation per 1k tokens (~$0.00015 input, ~$0.00060 output for mini models)
        double inputPricePerToken = 0.00000015;
        double outputPricePerToken = 0.00000060;
        double totalCost = (promptTokens * inputPricePerToken) + (completionTokens * outputPricePerToken);
        return BigDecimal.valueOf(totalCost).setScale(6, RoundingMode.HALF_UP);
    }

    public Map<String, Object> getStatus() {
        LlmProvider active = getActiveProvider();
        long totalTokens = usageLogRepository.getTotalTokensUsed();
        BigDecimal totalCost = usageLogRepository.getTotalCostUsd();

        return Map.of(
                "activeProvider", active.getProviderName(),
                "activeModel", active.getModelName(),
                "openaiConfigured", openAiProvider.isConfigured(),
                "geminiConfigured", geminiProvider.isConfigured(),
                "anthropicConfigured", anthropicProvider.isConfigured(),
                "totalTokensUsed", totalTokens,
                "totalCostUsd", totalCost != null ? totalCost : BigDecimal.ZERO
        );
    }
}