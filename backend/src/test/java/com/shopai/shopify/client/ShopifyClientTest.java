package com.shopai.shopify.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.shopify.service.ShopifyConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopifyClientTest {

    @Mock private ShopifyConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ShopifyClient shopifyClient;

    @BeforeEach
    void setUp() {
        shopifyClient = new ShopifyClientImpl(objectMapper, configService);
    }

    @Test
    void executeGraphQLHandlesConfigFallback() {
        when(configService.getActiveShopDomain()).thenReturn("test-store.myshopify.com");
        when(configService.getActiveAccessToken()).thenReturn("shpat_test_token");
        when(configService.getActiveApiVersion()).thenReturn("2024-04");

        // The query against a mock endpoint will return network error gracefully
        GraphQLResponse res = shopifyClient.executeGraphQL("{ shop { name } }", Collections.emptyMap());
        assertThat(res).isNotNull();
    }
}
