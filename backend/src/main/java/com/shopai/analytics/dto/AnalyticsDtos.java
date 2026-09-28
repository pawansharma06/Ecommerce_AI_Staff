package com.shopai.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalyticsDtos {

    public record AnalyticsOverviewDto(
            BigDecimal totalGmv,
            long totalOrders,
            BigDecimal averageOrderValue,
            long totalCustomers,
            long activeProducts,
            long unfulfilledOrders,
            long abandonedCheckouts,
            long totalAiConversations,
            long totalAiMessages,
            long totalTokensConsumed,
            BigDecimal estimatedLlmCostUsd,
            double pendingApprovalRate
    ) {}

    public record SalesTrendPointDto(
            LocalDate date,
            BigDecimal revenue,
            long orderCount,
            BigDecimal averageOrderValue
    ) {}

    public record AiMetricsDto(
            long totalTokens,
            long promptTokens,
            long completionTokens,
            BigDecimal estimatedCostUsd,
            double avgLatencyMs,
            double p95LatencyMs,
            long totalConversations,
            long totalMessages,
            Map<String, Long> toolInvocations,
            Map<String, Long> channelDistribution,
            Map<String, Long> approvalStats
    ) {}

    public record GenerateReportRequest(
            String timeframe,
            String focusArea,
            String customPrompt
    ) {}

    public record ExecutiveReportResponse(
            String title,
            String generatedAt,
            String timeframe,
            String executiveSummary,
            List<String> keyHighlights,
            List<String> operationalAlerts,
            List<String> strategicRecommendations,
            Map<String, Object> metricsSnapshot
    ) {}
}
