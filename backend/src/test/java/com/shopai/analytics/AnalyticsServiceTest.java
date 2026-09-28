package com.shopai.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.repository.ConversationMessageRepository;
import com.shopai.agent.repository.ConversationRepository;
import com.shopai.analytics.dto.AnalyticsDtos.*;
import com.shopai.analytics.service.AnalyticsService;
import com.shopai.approval.repository.ActionRequestRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.channel.repository.ChannelMessageRepository;
import com.shopai.customer.repository.CustomerMemoryRepository;
import com.shopai.llm.domain.LlmUsageLog;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.repository.LlmUsageLogRepository;
import com.shopai.llm.service.LlmService;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AbandonedCheckoutRepository abandonedCheckoutRepository;
    @Mock
    private CustomerMemoryRepository customerMemoryRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMessageRepository messageRepository;
    @Mock
    private LlmUsageLogRepository llmUsageLogRepository;
    @Mock
    private ActionRequestRepository actionRequestRepository;
    @Mock
    private ChannelMessageRepository channelMessageRepository;
    @Mock
    private LlmService llmService;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                orderRepository,
                productRepository,
                abandonedCheckoutRepository,
                customerMemoryRepository,
                conversationRepository,
                messageRepository,
                llmUsageLogRepository,
                actionRequestRepository,
                channelMessageRepository,
                llmService,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnAnalyticsOverview() {
        when(orderRepository.sumTotalSales()).thenReturn(BigDecimal.valueOf(1250.50));
        when(orderRepository.count()).thenReturn(5L);
        when(productRepository.countByStatus("ACTIVE")).thenReturn(10L);
        when(orderRepository.countByFulfillmentStatus("UNFULFILLED")).thenReturn(2L);
        when(abandonedCheckoutRepository.countByRecoveryStatus("ABANDONED")).thenReturn(3L);
        when(customerMemoryRepository.count()).thenReturn(4L);
        when(conversationRepository.count()).thenReturn(8L);
        when(messageRepository.count()).thenReturn(24L);
        when(llmUsageLogRepository.getTotalTokensUsed()).thenReturn(3500L);
        when(llmUsageLogRepository.getTotalCostUsd()).thenReturn(BigDecimal.valueOf(0.045));
        when(actionRequestRepository.countByStatus("PENDING")).thenReturn(1L);
        when(actionRequestRepository.count()).thenReturn(2L);

        AnalyticsOverviewDto overview = analyticsService.getOverview();

        assertThat(overview).isNotNull();
        assertThat(overview.totalGmv()).isEqualByComparingTo("1250.50");
        assertThat(overview.totalOrders()).isEqualTo(5L);
        assertThat(overview.averageOrderValue()).isEqualByComparingTo("250.10");
        assertThat(overview.activeProducts()).isEqualTo(10L);
        assertThat(overview.totalAiConversations()).isEqualTo(8L);
        assertThat(overview.totalTokensConsumed()).isEqualTo(3500L);
    }

    @Test
    void shouldReturnAiMetrics() {
        LlmUsageLog log = new LlmUsageLog("openai", "gpt-4o-mini", "CUSTOMER_SUPPORT", 150, 50, 450, BigDecimal.valueOf(0.001));
        when(llmUsageLogRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(log));
        when(llmUsageLogRepository.getTotalTokensUsed()).thenReturn(200L);
        when(llmUsageLogRepository.getTotalCostUsd()).thenReturn(BigDecimal.valueOf(0.001));
        when(conversationRepository.count()).thenReturn(2L);
        when(messageRepository.count()).thenReturn(6L);
        when(messageRepository.findAll()).thenReturn(Collections.emptyList());

        AiMetricsDto metrics = analyticsService.getAiMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.totalTokens()).isEqualTo(200L);
        assertThat(metrics.promptTokens()).isEqualTo(150L);
        assertThat(metrics.completionTokens()).isEqualTo(50L);
        assertThat(metrics.avgLatencyMs()).isEqualTo(450.0);
        assertThat(metrics.toolInvocations()).isNotEmpty();
    }

    @Test
    void shouldGenerateExecutiveReport() {
        when(orderRepository.sumTotalSales()).thenReturn(BigDecimal.valueOf(5000.00));
        when(orderRepository.count()).thenReturn(20L);
        when(productRepository.countByStatus("ACTIVE")).thenReturn(15L);
        when(orderRepository.countByFulfillmentStatus("UNFULFILLED")).thenReturn(3L);
        when(abandonedCheckoutRepository.countByRecoveryStatus("ABANDONED")).thenReturn(5L);
        when(conversationRepository.count()).thenReturn(12L);
        when(messageRepository.count()).thenReturn(40L);
        when(llmUsageLogRepository.getTotalTokensUsed()).thenReturn(5000L);
        when(llmUsageLogRepository.getTotalCostUsd()).thenReturn(BigDecimal.valueOf(0.12));

        when(llmService.generateChat(any(), any())).thenReturn(LlmResponse.text("Executive summary: Store is generating healthy growth.", 80, 20, "openai", "gpt-4o"));

        ExecutiveReportResponse report = analyticsService.generateExecutiveReport(new GenerateReportRequest("Last 30 Days", "Growth", "Focus on sales"));

        assertThat(report).isNotNull();
        assertThat(report.title()).contains("Executive Commerce & AI Intelligence Brief");
        assertThat(report.executiveSummary()).contains("growth");
        assertThat(report.keyHighlights()).hasSize(3);
        assertThat(report.operationalAlerts()).isNotEmpty();
        assertThat(report.strategicRecommendations()).hasSize(3);
    }
}
