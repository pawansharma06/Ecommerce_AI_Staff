package com.shopai.shopify;

import com.shopai.shopify.domain.WebhookEvent;
import com.shopai.shopify.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShopifyWebhookIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WebhookEventRepository webhookEventRepository;

    @Test
    void receivesAndIngestsShopifyWebhookEvent() throws Exception {
        String payload = """
                {
                  "id": 820982911948824508,
                  "email": "jon@example.com",
                  "total_price": "199.00"
                }
                """;

        mockMvc.perform(post("/api/v1/shopify/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Shopify-Topic", "orders/create")
                        .header("X-Shopify-Shop-Domain", "test-shop.myshopify.com")
                        .header("X-Shopify-API-Version", "2024-04")
                        .header("X-Shopify-Webhook-Id", "wh_test_" + System.currentTimeMillis())
                        .content(payload.getBytes()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Webhook received")));

        List<WebhookEvent> events = webhookEventRepository.findAll();
        assertThat(events).anyMatch(e -> "orders/create".equals(e.getTopic()) && "test-shop.myshopify.com".equals(e.getShopDomain()));
    }
}
