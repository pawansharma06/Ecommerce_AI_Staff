package com.shopai.shopify.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shopify_config")
public class ShopifyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shop_domain", nullable = false, unique = true)
    private String shopDomain;

    @Column(name = "access_token_encrypted", nullable = false)
    private String accessTokenEncrypted;

    @Column(name = "webhook_secret_encrypted")
    private String webhookSecretEncrypted;

    @Column(name = "api_version", nullable = false, length = 20)
    private String apiVersion = "2024-04";

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "shop_owner")
    private String shopOwner;

    private String email;

    @Column(length = 10)
    private String currency = "USD";

    @Column(length = 100)
    private String timezone = "UTC";

    @Column(nullable = false, length = 20)
    private String status = "CONFIGURED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ShopifyConfig() {}

    public ShopifyConfig(String shopDomain, String accessTokenEncrypted, String webhookSecretEncrypted, String apiVersion) {
        this.shopDomain = shopDomain;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.webhookSecretEncrypted = webhookSecretEncrypted;
        this.apiVersion = apiVersion != null ? apiVersion : "2024-04";
        this.status = "CONFIGURED";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getShopDomain() { return shopDomain; }
    public void setShopDomain(String shopDomain) { this.shopDomain = shopDomain; }

    public String getAccessTokenEncrypted() { return accessTokenEncrypted; }
    public void setAccessTokenEncrypted(String accessTokenEncrypted) { this.accessTokenEncrypted = accessTokenEncrypted; }

    public String getWebhookSecretEncrypted() { return webhookSecretEncrypted; }
    public void setWebhookSecretEncrypted(String webhookSecretEncrypted) { this.webhookSecretEncrypted = webhookSecretEncrypted; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopOwner() { return shopOwner; }
    public void setShopOwner(String shopOwner) { this.shopOwner = shopOwner; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
