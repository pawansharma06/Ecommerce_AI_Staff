package com.shopai.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.domain.ConversationMessage;
import com.shopai.agent.repository.ConversationMessageRepository;
import com.shopai.agent.repository.ConversationRepository;
import com.shopai.analytics.dto.AnalyticsDtos.*;
import com.shopai.approval.repository.ActionRequestRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.repository.ChannelMessageRepository;
import com.shopai.customer.repository.CustomerMemoryRepository;
import com.shopai.llm.domain.LlmUsageLog;
import com.shopai.llm.dto.LlmMessage;
import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.repository.LlmUsageLogRepository;
import com.shopai.llm.service.LlmService;
import com.shopai.order.domain.Order;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AbandonedCheckoutRepository abandonedCheckoutRepository;
    private final CustomerMemoryRepository customerMemoryRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final LlmUsageLogRepository llmUsageLogRepository;
    private final ActionRequestRepository actionRequestRepository;
    private final ChannelMessageRepository channelMessageRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public AnalyticsService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            AbandonedCheckoutRepository abandonedCheckoutRepository,
            CustomerMemoryRepository customerMemoryRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            LlmUsageLogRepository llmUsageLogRepository,
            ActionRequestRepository actionRequestRepository,
            ChannelMessageRepository channelMessageRepository,
            LlmService llmService,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.abandonedCheckoutRepository = abandonedCheckoutRepository;
        this.customerMemoryRepository = customerMemoryRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.llmUsageLogRepository = llmUsageLogRepository;
        this.actionRequestRepository = actionRequestRepository;
        this.channelMessageRepository = channelMessageRepository;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewDto getOverview() {
        BigDecimal totalGmv = orderRepository.sumTotalSales();
        if (totalGmv == null) totalGmv = BigDecimal.ZERO;

        long totalOrders = orderRepository.count();
        BigDecimal aov = totalOrders > 0
                ? totalGmv.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long activeProducts = productRepository.countByStatus("ACTIVE");
        long unfulfilledOrders = orderRepository.countByFulfillmentStatus("UNFULFILLED");
        long abandonedCheckouts = abandonedCheckoutRepository.countByRecoveryStatus("ABANDONED");

        long totalCustomers = customerMemoryRepository.count(); // estimated distinct customer profiles
        if (totalCustomers == 0 && totalOrders > 0) {
            totalCustomers = Math.max(1, totalOrders / 2);
        }

        long totalConversations = conversationRepository.count();
        long totalMessages = messageRepository.count();
        long totalTokens = llmUsageLogRepository.getTotalTokensUsed();
        BigDecimal llmCost = llmUsageLogRepository.getTotalCostUsd();
        if (llmCost == null) llmCost = BigDecimal.ZERO;

        long pendingApprovals = actionRequestRepository.countByStatus("PENDING");
        long totalApprovals = actionRequestRepository.count();
        double pendingRate = totalApprovals > 0 ? (double) pendingApprovals / totalApprovals : 0.0;

        return new AnalyticsOverviewDto(
                totalGmv,
                totalOrders,
                aov,
                totalCustomers,
                activeProducts,
                unfulfilledOrders,
                abandonedCheckouts,
                totalConversations,
                totalMessages,
                totalTokens,
                llmCost,
                pendingRate
        );
    }

    @Transactional(readOnly = true)
    public List<SalesTrendPointDto> getSalesTrends(int days) {
        if (days <= 0) days = 14;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Order> allOrders = orderRepository.findAll();
        Map<LocalDate, List<Order>> ordersByDate = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()));

        List<SalesTrendPointDto> points = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            List<Order> dayOrders = ordersByDate.getOrDefault(d, Collections.emptyList());
            BigDecimal dayRevenue = dayOrders.stream()
                    .map(Order::getTotalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long orderCount = dayOrders.size();
            BigDecimal aov = orderCount > 0
                    ? dayRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            points.add(new SalesTrendPointDto(d, dayRevenue, orderCount, aov));
        }

        return points;
    }

    @Transactional(readOnly = true)
    public AiMetricsDto getAiMetrics() {
        List<LlmUsageLog> recentLogs = llmUsageLogRepository.findTop50ByOrderByCreatedAtDesc();

        long totalTokens = llmUsageLogRepository.getTotalTokensUsed();
        long promptTokens = recentLogs.stream().mapToLong(LlmUsageLog::getPromptTokens).sum();
        long completionTokens = recentLogs.stream().mapToLong(LlmUsageLog::getCompletionTokens).sum();
        BigDecimal estimatedCost = llmUsageLogRepository.getTotalCostUsd();
        if (estimatedCost == null) estimatedCost = BigDecimal.ZERO;

        double avgLatency = recentLogs.stream().mapToLong(LlmUsageLog::getLatencyMs).average().orElse(0.0);
        List<Long> latencies = recentLogs.stream().map(LlmUsageLog::getLatencyMs).sorted().toList();
        double p95Latency = latencies.isEmpty() ? 0.0 : latencies.get((int) Math.min(latencies.size() - 1, Math.floor(latencies.size() * 0.95)));

        long totalConversations = conversationRepository.count();
        long totalMessages = messageRepository.count();

        // Tool Invocation stats
        Map<String, Long> toolInvocations = new LinkedHashMap<>();
        List<ConversationMessage> allMessages = messageRepository.findAll();
        for (ConversationMessage m : allMessages) {
            if (m.getToolCalls() != null && !m.getToolCalls().isBlank()) {
                try {
                    JsonNode toolArray = objectMapper.readTree(m.getToolCalls());
                    if (toolArray.isArray()) {
                        for (JsonNode toolNode : toolArray) {
                            String toolName = toolNode.path("function").path("name").asText(toolNode.path("name").asText());
                            if (!toolName.isBlank()) {
                                toolInvocations.put(toolName, toolInvocations.getOrDefault(toolName, 0L) + 1);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Channel Distribution
        Map<String, Long> channelDistribution = new LinkedHashMap<>();
        for (ChannelType type : ChannelType.values()) {
            long count = channelMessageRepository.countByChannelType(type);
            channelDistribution.put(type.name(), count);
        }

        // Approval stats
        Map<String, Long> approvalStats = new LinkedHashMap<>();
        approvalStats.put("PENDING", actionRequestRepository.countByStatus("PENDING"));
        approvalStats.put("APPROVED", actionRequestRepository.countByStatus("APPROVED"));
        approvalStats.put("REJECTED", actionRequestRepository.countByStatus("REJECTED"));
        approvalStats.put("EXECUTED", actionRequestRepository.countByStatus("EXECUTED"));

        return new AiMetricsDto(
                totalTokens,
                promptTokens,
                completionTokens,
                estimatedCost,
                Math.round(avgLatency * 10.0) / 10.0,
                Math.round(p95Latency * 10.0) / 10.0,
                totalConversations,
                totalMessages,
                toolInvocations,
                channelDistribution,
                approvalStats
        );
    }

    public ExecutiveReportResponse generateExecutiveReport(GenerateReportRequest request) {
        AnalyticsOverviewDto overview = getOverview();
        AiMetricsDto aiMetrics = getAiMetrics();
        String timeframe = request.timeframe() != null && !request.timeframe().isBlank() ? request.timeframe() : "Last 30 Days";

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("totalGmv", overview.totalGmv());
        snapshot.put("totalOrders", overview.totalOrders());
        snapshot.put("averageOrderValue", overview.averageOrderValue());
        snapshot.put("activeProducts", overview.activeProducts());
        snapshot.put("unfulfilledOrders", overview.unfulfilledOrders());
        snapshot.put("abandonedCheckouts", overview.abandonedCheckouts());
        snapshot.put("totalAiConversations", overview.totalAiConversations());
        snapshot.put("totalTokensConsumed", overview.totalTokensConsumed());

        String promptContext = String.format("""
                STORE METRICS SNAPSHOT:
                - Timeframe: %s
                - Gross Merchandise Value (GMV): $%s
                - Total Orders: %d
                - Average Order Value (AOV): $%s
                - Active Products: %d
                - Unfulfilled Orders: %d
                - Abandoned Checkouts: %d
                - Total AI Handled Conversations: %d
                - Total LLM Tokens Used: %d
                - Focus Area Requested: %s
                - Custom Directives: %s
                """,
                timeframe,
                overview.totalGmv(),
                overview.totalOrders(),
                overview.averageOrderValue(),
                overview.activeProducts(),
                overview.unfulfilledOrders(),
                overview.abandonedCheckouts(),
                overview.totalAiConversations(),
                overview.totalTokensConsumed(),
                request.focusArea() != null ? request.focusArea() : "General Business & AI Performance",
                request.customPrompt() != null ? request.customPrompt() : "None"
        );

        String systemPrompt = """
                You are ShopAI Executive Business & Operations Intelligence Analyst.
                Synthesize store metrics and AI assistant operations into an actionable, crisp executive brief.
                Generate:
                1. A clear 2-3 sentence Executive Summary.
                2. 3 Key Business Highlights.
                3. 2 Operational Alerts/Risks (e.g. unfulfilled orders, inventory, or cart abandonment).
                4. 3 Strategic Action Recommendations.
                """;

        LlmPrompt llmPrompt = new LlmPrompt(
                systemPrompt,
                List.of(LlmMessage.user("Analyze the following store metrics and produce the executive report:\n" + promptContext)),
                0.7,
                1024
        );

        LlmResponse llmResponse = llmService.generateChat(llmPrompt, "EXECUTIVE_ANALYTICS");
        String reportText = llmResponse != null ? llmResponse.content() : "";

        // Extract structured sections or build defaults
        List<String> highlights = List.of(
                String.format("Store generated $%s in total sales across %d fulfilled orders.", overview.totalGmv(), overview.totalOrders()),
                String.format("AI Autonomous Engine resolved %d customer sessions with %d total tokens consumed.", overview.totalAiConversations(), overview.totalTokensConsumed()),
                String.format("Average Order Value currently stands at $%s.", overview.averageOrderValue())
        );

        List<String> alerts = new ArrayList<>();
        if (overview.unfulfilledOrders() > 0) {
            alerts.add(String.format("Action required on %d unfulfilled orders to prevent shipping delays.", overview.unfulfilledOrders()));
        }
        if (overview.abandonedCheckouts() > 0) {
            alerts.add(String.format("%d checkouts abandoned. Autonomous recovery campaigns recommended.", overview.abandonedCheckouts()));
        }
        if (alerts.isEmpty()) {
            alerts.add("All order fulfillment queues and cart recovery workflows are operating normally.");
        }

        List<String> recommendations = List.of(
                "Activate automated cart abandonment recovery sequences across WhatsApp and Email channels.",
                "Leverage Frequently-Bought-Together product recommendations in storefront chat to boost AOV.",
                "Review high-risk tool approval logs to streamline refund and inventory adjustment policies."
        );

        return new ExecutiveReportResponse(
                "ShopAI Executive Commerce & AI Intelligence Brief",
                Instant.now().toString(),
                timeframe,
                reportText != null && !reportText.isBlank() ? reportText : "Executive analysis generated based on store performance telemetry.",
                highlights,
                alerts,
                recommendations,
                snapshot
        );
    }
}
