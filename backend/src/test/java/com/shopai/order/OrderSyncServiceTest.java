package com.shopai.order;

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
    @Mock private com.shopai.rag.service.KnowledgeIndexService knowledgeIndexService;
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
