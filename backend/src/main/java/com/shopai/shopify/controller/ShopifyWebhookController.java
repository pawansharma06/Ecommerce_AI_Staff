package com.shopai.shopify.controller;

import com.shopai.shopify.domain.WebhookEvent;
import com.shopai.shopify.webhook.ShopifyWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shopify/webhooks")
@Tag(name = "Shopify Webhooks", description = "Shopify Webhook Ingestion Endpoint")
public class ShopifyWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ShopifyWebhookController.class);

    private final ShopifyWebhookService webhookService;

    public ShopifyWebhookController(ShopifyWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    @Operation(summary = "Receive and verify Shopify webhook event")
    public ResponseEntity<String> receiveWebhook(
            @RequestBody byte[] payload,
            @RequestHeader(value = "X-Shopify-Topic", required = false) String topic,
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmacHeader,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId
    ) {
        log.debug("Received webhook: topic={}, shop={}, webhookId={}", topic, shopDomain, webhookId);
        WebhookEvent event = webhookService.handleIncomingWebhook(payload, topic, hmacHeader, shopDomain, apiVersion, webhookId);
        return ResponseEntity.ok("Webhook received: " + event.getId());
    }
}
