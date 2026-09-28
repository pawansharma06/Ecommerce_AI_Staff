package com.shopai.shopify.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.catalog.service.CatalogSyncService;
import com.shopai.common.exception.ShopAiException;
import com.shopai.order.service.OrderSyncService;
import com.shopai.shopify.domain.WebhookEvent;
import com.shopai.shopify.repository.WebhookEventRepository;
import com.shopai.shopify.service.ShopifyConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ShopifyWebhookService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyWebhookService.class);

    private final ShopifyWebhookVerifier webhookVerifier;
    private final ShopifyConfigService configService;
    private final WebhookEventRepository webhookEventRepository;
    private final CatalogSyncService catalogSyncService;
    private final OrderSyncService orderSyncService;
    private final ObjectMapper objectMapper;

    public ShopifyWebhookService(
            ShopifyWebhookVerifier webhookVerifier,
            ShopifyConfigService configService,
            WebhookEventRepository webhookEventRepository,
            CatalogSyncService catalogSyncService,
            OrderSyncService orderSyncService,
            ObjectMapper objectMapper
    ) {
        this.webhookVerifier = webhookVerifier;
        this.configService = configService;
        this.webhookEventRepository = webhookEventRepository;
        this.catalogSyncService = catalogSyncService;
        this.orderSyncService = orderSyncService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WebhookEvent handleIncomingWebhook(
            byte[] rawPayload,
            String topic,
            String hmacHeader,
            String shopDomain,
            String apiVersion,
            String webhookId
    ) {
        String webhookSecret = configService.getActiveWebhookSecret();

        if (webhookSecret != null && !webhookSecret.isBlank()) {
            boolean valid = webhookVerifier.verifyHmac(rawPayload, hmacHeader, webhookSecret);
            if (!valid) {
                log.warn("Unauthorized Shopify webhook: invalid HMAC signature for topic {} from {}", topic, shopDomain);
                throw new ShopAiException("INVALID_WEBHOOK_HMAC", "Invalid Shopify webhook HMAC signature", HttpStatus.UNAUTHORIZED);
            }
        }

        // Check for idempotency
        if (webhookId != null && !webhookId.isBlank()) {
            Optional<WebhookEvent> existing = webhookEventRepository.findByShopifyWebhookId(webhookId);
            if (existing.isPresent()) {
                log.info("Duplicate webhook event ignored (id: {})", webhookId);
                return existing.get();
            }
        }

        String payloadStr = new String(rawPayload, StandardCharsets.UTF_8);
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "2024-04";

        WebhookEvent event = new WebhookEvent(
                topic != null ? topic : "unknown",
                webhookId,
                shopDomain != null ? shopDomain : configService.getActiveShopDomain(),
                version,
                payloadStr
        );

        event = webhookEventRepository.save(event);
        log.info("Ingested Shopify webhook event [{}] for shop: {}", topic, shopDomain);

        // Async processing dispatch
        processWebhookAsync(event.getId());

        return event;
    }

    @Async
    @Transactional
    public void processWebhookAsync(java.util.UUID eventId) {
        try {
            webhookEventRepository.findById(eventId).ifPresent(event -> {
                log.debug("Processing webhook async: {} ({})", event.getTopic(), event.getId());
                try {
                    JsonNode node = objectMapper.readTree(event.getPayload());
                    String topic = event.getTopic().toLowerCase();

                    if (topic.startsWith("products/create") || topic.startsWith("products/update")) {
                        catalogSyncService.processWebhookProductUpsert(node);
                    } else if (topic.startsWith("products/delete")) {
                        catalogSyncService.processWebhookProductDelete(node);
                    } else if (topic.startsWith("orders/")) {
                        orderSyncService.processWebhookOrderUpsert(node);
                    } else if (topic.startsWith("checkouts/")) {
                        orderSyncService.processWebhookCheckoutUpsert(node);
                    }

                    event.setStatus("PROCESSED");
                } catch (Exception parseEx) {
                    log.error("Failed to parse and dispatch webhook payload: {}", parseEx.getMessage(), parseEx);
                    event.setStatus("FAILED");
                }
                event.setProcessedAt(Instant.now());
                webhookEventRepository.save(event);
            });
        } catch (Exception e) {
            log.error("Failed to process webhook asynchronously: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<WebhookEvent> getRecentEvents() {
        return webhookEventRepository.findTop50ByOrderByCreatedAtDesc();
    }
}

