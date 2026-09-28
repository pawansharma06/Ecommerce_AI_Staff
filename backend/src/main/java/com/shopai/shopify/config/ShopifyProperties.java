package com.shopai.shopify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "shopify")
public class ShopifyProperties {

    private String shopDomain;
    private String adminAccessToken;
    private String webhookSecret;
    private String apiVersion = "2024-04";

    public String getShopDomain() { return shopDomain; }
    public void setShopDomain(String shopDomain) { this.shopDomain = shopDomain; }

    public String getAdminAccessToken() { return adminAccessToken; }
    public void setAdminAccessToken(String adminAccessToken) { this.adminAccessToken = adminAccessToken; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
}
