package com.shopai.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class ShopAiMetricsService {

    private final MeterRegistry meterRegistry;

    public ShopAiMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLlmRequest(String provider, String model, String agentName, boolean success, long latencyMs, int promptTokens, int completionTokens) {
        Counter.builder("shopai.llm.requests")
                .tag("provider", provider != null ? provider : "unknown")
                .tag("model", model != null ? model : "unknown")
                .tag("agent", agentName != null ? agentName : "default")
                .tag("status", success ? "SUCCESS" : "FAILURE")
                .description("Total count of LLM provider generation requests")
                .register(meterRegistry)
                .increment();

        Counter.builder("shopai.llm.tokens")
                .tag("type", "prompt")
                .tag("provider", provider != null ? provider : "unknown")
                .description("Total prompt tokens consumed")
                .register(meterRegistry)
                .increment(promptTokens);

        Counter.builder("shopai.llm.tokens")
                .tag("type", "completion")
                .tag("provider", provider != null ? provider : "unknown")
                .description("Total completion tokens consumed")
                .register(meterRegistry)
                .increment(completionTokens);

        Timer.builder("shopai.llm.latency")
                .tag("provider", provider != null ? provider : "unknown")
                .tag("agent", agentName != null ? agentName : "default")
                .description("LLM generation latency in seconds")
                .register(meterRegistry)
                .record(Duration.ofMillis(latencyMs));
    }

    public void recordToolExecution(String toolName, String status, String riskLevel, long executionMs) {
        Counter.builder("shopai.tools.executed")
                .tag("tool", toolName != null ? toolName : "unknown")
                .tag("status", status != null ? status : "SUCCESS")
                .tag("risk", riskLevel != null ? riskLevel : "LOW")
                .description("Total count of agent tool executions")
                .register(meterRegistry)
                .increment();

        Timer.builder("shopai.tools.latency")
                .tag("tool", toolName != null ? toolName : "unknown")
                .description("Agent tool execution latency")
                .register(meterRegistry)
                .record(executionMs, TimeUnit.MILLISECONDS);
    }

    public void recordChannelMessage(String channelType, String direction, String status) {
        Counter.builder("shopai.channel.messages")
                .tag("channel", channelType != null ? channelType : "unknown")
                .tag("direction", direction != null ? direction : "INBOUND")
                .tag("status", status != null ? status : "PROCESSED")
                .description("Total count of multi-channel messages processed")
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderSynced(String status) {
        Counter.builder("shopai.orders.synced")
                .tag("status", status != null ? status : "SUCCESS")
                .description("Total count of Shopify orders synced")
                .register(meterRegistry)
                .increment();
    }
}
