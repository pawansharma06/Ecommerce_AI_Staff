package com.shopai.shopify.service;

import com.shopai.common.crypto.EncryptionService;
import com.shopai.shopify.client.ShopifyClient;
import com.shopai.shopify.config.ShopifyProperties;
import com.shopai.shopify.domain.ShopifyConfig;
import com.shopai.shopify.dto.ShopifyConfigRequest;
import com.shopai.shopify.dto.ShopifyConfigResponse;
import com.shopai.shopify.repository.ShopifyConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopifyConfigServiceTest {

    @Mock private ShopifyConfigRepository configRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private ShopifyClient shopifyClient;
    private ShopifyProperties properties;

    private ShopifyConfigService configService;

    @BeforeEach
    void setUp() {
        properties = new ShopifyProperties();
        properties.setShopDomain("env-store.myshopify.com");
        properties.setAdminAccessToken("shpat_env_token");
        properties.setApiVersion("2024-04");

        configService = new ShopifyConfigService(configRepository, properties, encryptionService, shopifyClient);
    }

    @Test
    void fallsBackToPropertiesWhenDatabaseIsEmpty() {
        when(configRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        assertThat(configService.getActiveShopDomain()).isEqualTo("env-store.myshopify.com");
        assertThat(configService.getActiveAccessToken()).isEqualTo("shpat_env_token");
        assertThat(configService.getActiveApiVersion()).isEqualTo("2024-04");
    }

    @Test
    void savesAndEncryptsNewConfiguration() {
        ShopifyConfigRequest request = new ShopifyConfigRequest(
                "my-shop.myshopify.com",
                "shpat_new_secret_token",
                "shpss_webhook_secret",
                "2024-04"
        );

        when(configRepository.findByShopDomain("my-shop.myshopify.com")).thenReturn(Optional.empty());
        when(encryptionService.encrypt("shpat_new_secret_token")).thenReturn("enc_token");
        when(encryptionService.encrypt("shpss_webhook_secret")).thenReturn("enc_secret");
        when(shopifyClient.getShopDetails("my-shop.myshopify.com", "shpat_new_secret_token", "2024-04"))
                .thenReturn(Map.of("name", "My Super Store", "currencyCode", "USD", "timezoneAbbreviation", "EST"));

        when(configRepository.save(any(ShopifyConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ShopifyConfigResponse response = configService.saveOrUpdateConfig(request);

        assertThat(response).isNotNull();
        assertThat(response.shopDomain()).isEqualTo("my-shop.myshopify.com");
        assertThat(response.shopName()).isEqualTo("My Super Store");
        assertThat(response.status()).isEqualTo("CONNECTED");

        verify(configRepository).save(any(ShopifyConfig.class));
    }
}
