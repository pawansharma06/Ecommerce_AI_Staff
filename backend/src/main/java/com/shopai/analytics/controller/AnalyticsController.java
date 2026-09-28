package com.shopai.analytics.controller;

import com.shopai.analytics.dto.AnalyticsDtos.*;
import com.shopai.analytics.service.AnalyticsService;
import com.shopai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics & AI Reporting", description = "Commerce metrics, sales trends, AI token economics, and natural language report generation")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get high-level commerce & AI operational KPIs")
    public ResponseEntity<ApiResponse<AnalyticsOverviewDto>> getOverview() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getOverview()));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get historical daily sales trends")
    public ResponseEntity<ApiResponse<List<SalesTrendPointDto>>> getSalesTrends(
            @RequestParam(defaultValue = "14") int days
    ) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getSalesTrends(days)));
    }

    @GetMapping("/ai-metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get AI token consumption, latency profiling, and tool invocation distribution")
    public ResponseEntity<ApiResponse<AiMetricsDto>> getAiMetrics() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getAiMetrics()));
    }

    @PostMapping("/report/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate an AI-powered natural language executive business brief")
    public ResponseEntity<ApiResponse<ExecutiveReportResponse>> generateReport(
            @RequestBody(required = false) GenerateReportRequest request
    ) {
        GenerateReportRequest safeReq = request != null ? request : new GenerateReportRequest("Last 30 Days", "General", "");
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.generateExecutiveReport(safeReq)));
    }
}
