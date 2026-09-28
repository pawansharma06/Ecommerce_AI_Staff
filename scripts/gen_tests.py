import os

def write_test_file(rel_path, content):
    full_path = os.path.join(r'c:\apps\ShopAI\backend\src\test\java\com\shopai', rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print('Created Test:', rel_path)

# ==============================================================================
# CATALOG & ORDER SYNC TESTS
# ==============================================================================

write_test_file('catalog/CatalogSyncServiceTest.java', '''package com.shopai.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.CollectionRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.catalog.repository.ProductVariantRepository;
import com.shopai.catalog.service.CatalogSyncService;
import com.shopai.shopify.client.GraphQLResponse;
import com.shopai.shopify.client.ShopifyClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock private ShopifyClient shopifyClient;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private CollectionRepository collectionRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CatalogSyncService catalogSyncService;

    @Test
    void testPerformFullCatalogSync() throws Exception {
        String json = """
        {
          "data": {
            "products": {
              "pageInfo": { "hasNextPage": false, "endCursor": null },
              "edges": [
                {
                  "node": {
                    "id": "gid://shopify/Product/987654321",
                    "title": "Snowboard Supreme",
                    "handle": "snowboard-supreme",
                    "descriptionHtml": "<p>Awesome board</p>",
                    "vendor": "ShopAI Snow",
                    "productType": "Snowboards",
                    "status": "ACTIVE",
                    "tags": ["winter", "sports"],
                    "totalInventory": 15,
                    "variants": {
                      "edges": [
                        {
                          "node": {
                            "id": "gid://shopify/ProductVariant/11223344",
                            "title": "156cm",
                            "sku": "SNOW-156",
                            "price": "599.99",
                            "inventoryQuantity": 15
                          }
                        }
                      ]
                    },
                    "collections": {
                      "edges": []
                    }
                  }
                }
              ]
            }
          }
        }
        """;

        JsonNode rootNode = objectMapper.readTree(json);
        GraphQLResponse gqlResponse = GraphQLResponse.success(rootNode.path("data"), 10, 990, 50);

        when(shopifyClient.executeGraphQL(anyString(), any(Map.class))).thenReturn(gqlResponse);
        when(productRepository.findByShopifyProductId(987654321L)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        int processed = catalogSyncService.performFullCatalogSync(null);

        assertThat(processed).isEqualTo(1);
        verify(productRepository).save(any(Product.class));
        verify(productVariantRepository).save(any());
    }
}
''')

write_test_file('order/OrderSyncServiceTest.java', '''package com.shopai.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.order.domain.Order;
import com.shopai.order.repository.*;
import com.shopai.order.service.OrderSyncService;
import com.shopai.shopify.client.GraphQLResponse;
import com.shopai.shopify.client.ShopifyClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSyncServiceTest {

    @Mock private ShopifyClient shopifyClient;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderLineItemRepository orderLineItemRepository;
    @Mock private FulfillmentRepository fulfillmentRepository;
    @Mock private OrderTransactionRepository orderTransactionRepository;
    @Mock private AbandonedCheckoutRepository abandonedCheckoutRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderSyncService orderSyncService;

    @Test
    void testPerformFullOrderSyncWithFulfillmentsAndTransactions() throws Exception {
        String json = """
        {
          "data": {
            "orders": {
              "pageInfo": { "hasNextPage": false, "endCursor": null },
              "edges": [
                {
                  "node": {
                    "id": "gid://shopify/Order/1001",
                    "name": "#1001",
                    "email": "customer@example.com",
                    "displayFinancialStatus": "PAID",
                    "displayFulfillmentStatus": "FULFILLED",
                    "currencyCode": "USD",
                    "totalPriceSet": { "presentmentMoney": { "amount": "129.99", "currencyCode": "USD" } },
                    "lineItems": {
                      "edges": [
                        {
                          "node": {
                            "id": "gid://shopify/LineItem/5001",
                            "title": "Winter Jacket",
                            "quantity": 1,
                            "discountedUnitPriceAfterAllDiscountsSet": { "presentmentMoney": { "amount": "129.99" } }
                          }
                        }
                      ]
                    },
                    "fulfillments": [
                      {
                        "id": "gid://shopify/Fulfillment/7001",
                        "status": "SUCCESS",
                        "trackingInfo": [
                          {
                            "company": "USPS",
                            "number": "9400111899562537624991",
                            "url": "https://tools.usps.com/go/TrackConfirmAction?tLabels=9400111899562537624991"
                          }
                        ]
                      }
                    ],
                    "transactions": [
                      {
                        "id": "gid://shopify/OrderTransaction/8001",
                        "kind": "SALE",
                        "status": "SUCCESS",
                        "gateway": "shopify_payments",
                        "amountSet": { "presentmentMoney": { "amount": "129.99", "currencyCode": "USD" } }
                      }
                    ]
                  }
                }
              ]
            }
          }
        }
        """;

        JsonNode rootNode = objectMapper.readTree(json);
        GraphQLResponse gqlResponse = GraphQLResponse.success(rootNode.path("data"), 15, 985, 50);

        when(shopifyClient.executeGraphQL(anyString(), any(Map.class))).thenReturn(gqlResponse);
        when(orderRepository.findByShopifyOrderId(1001L)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        int processed = orderSyncService.performFullOrderSync(null);

        assertThat(processed).isEqualTo(1);
        verify(orderRepository).save(any(Order.class));
        verify(orderLineItemRepository).save(any());
        verify(fulfillmentRepository).save(any());
        verify(orderTransactionRepository).save(any());
    }
}
''')

# ==============================================================================
# CONTROLLER INTEGRATION TESTS
# ==============================================================================

write_test_file('catalog/ProductControllerIntegrationTest.java', '''package com.shopai.catalog;

import com.shopai.auth.security.CookieUtils;
import com.shopai.auth.security.JwtService;
import com.shopai.catalog.domain.Product;
import com.shopai.catalog.domain.ProductVariant;
import com.shopai.catalog.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private JwtService jwtService;

    private Cookie adminAuthCookie;

    @BeforeEach
    void setUp() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "admin@shopai.dev", List.of("ADMIN"), List.of("product.read", "product.update"));
        adminAuthCookie = new Cookie(CookieUtils.ACCESS_TOKEN_COOKIE, token);

        Product p = new Product(10001L, "Sample Hoodie", "sample-hoodie");
        p.setVendor("ShopAI");
        p.setStatus("ACTIVE");
        p.setTotalInventory(20);

        ProductVariant v = new ProductVariant(p, 20001L, "L", new BigDecimal("49.99"));
        v.setInventoryQuantity(20);
        p.addVariant(v);

        productRepository.save(p);
    }

    @Test
    void testGetProductsAndStats() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].title", notNullValue()));

        mockMvc.perform(get("/api/v1/products/stats")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProducts", greaterThanOrEqualTo(1)));
    }
}
''')

write_test_file('order/OrderControllerIntegrationTest.java', '''package com.shopai.order;

import com.shopai.auth.security.CookieUtils;
import com.shopai.auth.security.JwtService;
import com.shopai.order.domain.AbandonedCheckout;
import com.shopai.order.domain.Order;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.order.repository.OrderRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderRepository orderRepository;
    @Autowired private AbandonedCheckoutRepository abandonedCheckoutRepository;
    @Autowired private JwtService jwtService;

    private Cookie adminAuthCookie;

    @BeforeEach
    void setUp() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "admin@shopai.dev", List.of("ADMIN"), List.of("order.read"));
        adminAuthCookie = new Cookie(CookieUtils.ACCESS_TOKEN_COOKIE, token);

        Order o = new Order(555L, "1005", "#1005");
        o.setFinancialStatus("PAID");
        o.setFulfillmentStatus("UNFULFILLED");
        o.setTotalPrice(new BigDecimal("99.00"));
        orderRepository.save(o);

        AbandonedCheckout a = new AbandonedCheckout(888L, "test@shopai.dev", new BigDecimal("150.00"));
        abandonedCheckoutRepository.save(a);
    }

    @Test
    void testGetOrdersAndAbandonedCheckouts() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/orders/stats")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalOrders", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/abandoned-checkouts")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
''')

print('Generated Phase 4 Unit & Integration Tests.')

