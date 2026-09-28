package com.shopai.shopify.service;

import com.shopai.common.crypto.EncryptionService;
import com.shopai.common.exception.ShopAiException;
import com.shopai.shopify.client.ShopifyClient;
import com.shopai.shopify.config.ShopifyProperties;
import com.shopai.shopify.domain.ShopifyConfig;
import com.shopai.shopify.dto.ShopifyConfigRequest;
import com.shopai.shopify.dto.ShopifyConfigResponse;
import com.shopai.shopify.dto.ShopifyTestResponse;
import com.shopai.shopify.repository.ShopifyConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class ShopifyConfigService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyConfigService.class);

    private final ShopifyConfigRepository configRepository;
    private final ShopifyProperties properties;
    private final EncryptionService encryptionService;
    private final ShopifyClient shopifyClient;

    public ShopifyConfigService(
            ShopifyConfigRepository configRepository,
            ShopifyProperties properties,
            EncryptionService encryptionService,
            @Lazy ShopifyClient shopifyClient
    ) {
        this.configRepository = configRepository;
        this.properties = properties;
        this.encryptionService = encryptionService;
        this.shopifyClient = shopifyClient;
    }

    @PostConstruct
    @Transactional
    public void initFromEnvIfConfigured() {
        if (properties.getShopDomain() != null && !properties.getShopDomain().isBlank()
                && properties.getAdminAccessToken() != null && !properties.getAdminAccessToken().isBlank()) {
            String domain = normalizeDomain(properties.getShopDomain());
            Optional<ShopifyConfig> existing = configRepository.findByShopDomain(domain);
            if (existing.isEmpty()) {
                log.info("Auto-configuring Shopify store from environment: {}", domain);
                ShopifyConfig config = new ShopifyConfig(
                        domain,
                        encryptionService.encrypt(properties.getAdminAccessToken()),
                        properties.getWebhookSecret() != null ? encryptionService.encrypt(properties.getWebhookSecret()) : null,
                        properties.getApiVersion()
                );
                configRepository.save(config);
            }
        }
    }

    public String getActiveShopDomain() {
        Optional<ShopifyConfig> config = configRepository.findFirstByOrderByCreatedAtDesc();
        if (config.isPresent()) {
            return config.get().getShopDomain();
        }
        return properties.getShopDomain() != null ? normalizeDomain(properties.getShopDomain()) : null;
    }

    public String getActiveAccessToken() {
        Optional<ShopifyConfig> config = configRepository.findFirstByOrderByCreatedAtDesc();
        if (config.isPresent()) {
            return encryptionService.decrypt(config.get().getAccessTokenEncrypted());
        }
        return properties.getAdminAccessToken();
    }

    public String getActiveWebhookSecret() {
        Optional<ShopifyConfig> config = configRepository.findFirstByOrderByCreatedAtDesc();
        if (config.isPresent() && config.get().getWebhookSecretEncrypted() != null) {
            return encryptionService.decrypt(config.get().getWebhookSecretEncrypted());
        }
        return properties.getWebhookSecret();
    }

    public String getActiveApiVersion() {
        Optional<ShopifyConfig> config = configRepository.findFirstByOrderByCreatedAtDesc();
        return config.map(ShopifyConfig::getApiVersion).orElseGet(properties::getApiVersion);
    }

    @Transactional(readOnly = true)
    public ShopifyConfigResponse getConfig() {
        Optional<ShopifyConfig> configOpt = configRepository.findFirstByOrderByCreatedAtDesc();
        if (configOpt.isPresent()) {
            return toResponse(configOpt.get());
        }

        // Fallback info from properties
        boolean hasDomain = properties.getShopDomain() != null && !properties.getShopDomain().isBlank();
        boolean hasToken = properties.getAdminAccessToken() != null && !properties.getAdminAccessToken().isBlank();
        boolean hasSecret = properties.getWebhookSecret() != null && !properties.getWebhookSecret().isBlank();

        return new ShopifyConfigResponse(
                null,
                hasDomain ? normalizeDomain(properties.getShopDomain()) : "",
                hasToken,
                hasSecret,
                properties.getApiVersion(),
                null,
                null,
                null,
                "USD",
                "UTC",
                hasDomain && hasToken ? "CONFIGURED" : "DISCONNECTED",
                null,
                null
        );
    }

    @Transactional
    public ShopifyConfigResponse saveOrUpdateConfig(ShopifyConfigRequest request) {
        String cleanDomain = normalizeDomain(request.shopDomain());
        String version = (request.apiVersion() != null && !request.apiVersion().isBlank()) ? request.apiVersion() : "2024-04";

        ShopifyConfig config = configRepository.findByShopDomain(cleanDomain)
                .orElseGet(() -> new ShopifyConfig(cleanDomain, "", null, version));

        config.setShopDomain(cleanDomain);
        config.setAccessTokenEncrypted(encryptionService.encrypt(request.adminAccessToken()));
        if (request.webhookSecret() != null && !request.webhookSecret().isBlank()) {
            config.setWebhookSecretEncrypted(encryptionService.encrypt(request.webhookSecret()));
        }
        config.setApiVersion(version);

        // Attempt live sync of shop details
        try {
            Map<String, Object> details = shopifyClient.getShopDetails(cleanDomain, request.adminAccessToken(), version);
            config.setShopName((String) details.get("name"));
            config.setEmail((String) details.get("email"));
            config.setCurrency((String) details.get("currencyCode"));
            config.setTimezone((String) details.get("timezoneAbbreviation"));
            config.setStatus("CONNECTED");
        } catch (Exception e) {
            log.warn("Could not query shop metadata during save (will save as CONFIGURED): {}", e.getMessage());
            config.setStatus("CONFIGURED");
        }

        config = configRepository.save(config);
        return toResponse(config);
    }

    @Transactional
    public ShopifyTestResponse testConnection() {
        String domain = getActiveShopDomain();
        String token = getActiveAccessToken();
        String version = getActiveApiVersion();

        if (domain == null || token == null) {
            return new ShopifyTestResponse(false, "Shopify credentials are not configured", null);
        }

        try {
            Map<String, Object> details = shopifyClient.getShopDetails(domain, token, version);

            // Update status in DB
            configRepository.findByShopDomain(domain).ifPresent(cfg -> {
                cfg.setShopName((String) details.get("name"));
                cfg.setEmail((String) details.get("email"));
                cfg.setCurrency((String) details.get("currencyCode"));
                cfg.setTimezone((String) details.get("timezoneAbbreviation"));
                cfg.setStatus("CONNECTED");
                configRepository.save(cfg);
            });

            return new ShopifyTestResponse(true, "Successfully connected to Shopify store: " + details.get("name"), details);
        } catch (Exception e) {
            return new ShopifyTestResponse(false, "Connection failed: " + e.getMessage(), null);
        }
    }

    public ShopifyConfigResponse toResponse(ShopifyConfig config) {
        return new ShopifyConfigResponse(
                config.getId(),
                config.getShopDomain(),
                config.getAccessTokenEncrypted() != null && !config.getAccessTokenEncrypted().isBlank(),
                config.getWebhookSecretEncrypted() != null && !config.getWebhookSecretEncrypted().isBlank(),
                config.getApiVersion(),
                config.getShopName(),
                config.getShopOwner(),
                config.getEmail(),
                config.getCurrency(),
                config.getTimezone(),
                config.getStatus(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    private String normalizeDomain(String domain) {
        if (domain == null) return "";
        String clean = domain.trim().toLowerCase();
        clean = clean.replace("https://", "").replace("http://", "");
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }
}
