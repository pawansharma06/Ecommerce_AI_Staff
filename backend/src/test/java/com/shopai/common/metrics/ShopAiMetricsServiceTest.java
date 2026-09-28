package com.shopai.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShopAiMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private ShopAiMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new ShopAiMetricsService(meterRegistry);
    }

    @Test
    void shouldRecordLlmRequestMetrics() {
        metricsService.recordLlmRequest("OPENAI", "gpt-4o", "CUSTOMER_SUPPORT", true, 450, 100, 50);

        assertThat(meterRegistry.find("shopai.llm.requests").counter()).isNotNull();
        assertThat(meterRegistry.find("shopai.llm.requests").counter().count()).isEqualTo(1.0);

        assertThat(meterRegistry.find("shopai.llm.tokens").tag("type", "prompt").counter().count()).isEqualTo(100.0);
        assertThat(meterRegistry.find("shopai.llm.tokens").tag("type", "completion").counter().count()).isEqualTo(50.0);
        assertThat(meterRegistry.find("shopai.llm.latency").timer()).isNotNull();
    }

    @Test
    void shouldRecordToolExecutionMetrics() {
        metricsService.recordToolExecution("search_products", "SUCCESS", "LOW", 35);

        assertThat(meterRegistry.find("shopai.tools.executed").counter()).isNotNull();
        assertThat(meterRegistry.find("shopai.tools.executed").counter().count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordChannelAndOrderMetrics() {
        metricsService.recordChannelMessage("WHATSAPP", "INBOUND", "RECEIVED");
        metricsService.recordOrderSynced("SUCCESS");

        assertThat(meterRegistry.find("shopai.channel.messages").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("shopai.orders.synced").counter().count()).isEqualTo(1.0);
    }
}
