package com.shopai.shopify.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.shopify.dto.ShopifyConfigRequest;
import com.shopai.shopify.dto.ShopifyConfigResponse;
import com.shopai.shopify.dto.ShopifyTestResponse;
import com.shopai.shopify.dto.WebhookEventResponse;
import com.shopai.shopify.service.ShopifyConfigService;
import com.shopai.shopify.webhook.ShopifyWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shopify/config")
@Tag(name = "Shopify Configuration", description = "Shopify Custom/Private App configuration and status")
public class ShopifyConfigController {

    private final ShopifyConfigService configService;
    private final ShopifyWebhookService webhookService;

    public ShopifyConfigController(ShopifyConfigService configService, ShopifyWebhookService webhookService) {
        this.configService = configService;
        this.webhookService = webhookService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get current Shopify store connection configuration and status")
    public ResponseEntity<ApiResponse<ShopifyConfigResponse>> getConfig() {
        ShopifyConfigResponse config = configService.getConfig();
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Save or update Shopify Custom App credentials")
    public ResponseEntity<ApiResponse<ShopifyConfigResponse>> updateConfig(@Valid @RequestBody ShopifyConfigRequest request) {
        ShopifyConfigResponse config = configService.saveOrUpdateConfig(request);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Test live Shopify GraphQL connection")
    public ResponseEntity<ApiResponse<ShopifyTestResponse>> testConnection() {
        ShopifyTestResponse testResult = configService.testConnection();
        return ResponseEntity.ok(ApiResponse.ok(testResult));
    }

    @GetMapping("/webhooks")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "List recent Shopify webhook events")
    public ResponseEntity<ApiResponse<List<WebhookEventResponse>>> getRecentWebhooks() {
        List<WebhookEventResponse> events = webhookService.getRecentEvents().stream()
                .map(e -> new WebhookEventResponse(
                        e.getId(),
                        e.getTopic(),
                        e.getShopifyWebhookId(),
                        e.getShopDomain(),
                        e.getApiVersion(),
                        e.getStatus(),
                        e.getAttempts(),
                        e.getErrorMessage(),
                        e.getProcessedAt(),
                        e.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(events));
    }
}
