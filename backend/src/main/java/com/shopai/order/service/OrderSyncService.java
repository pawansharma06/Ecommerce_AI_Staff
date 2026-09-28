package com.shopai.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.order.domain.*;
import com.shopai.order.repository.*;
import com.shopai.shopify.client.ShopifyClient;
import com.shopai.sync.domain.SyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderSyncService {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncService.class);

    private final ShopifyClient shopifyClient;
    private final OrderRepository orderRepository;
    private final OrderLineItemRepository orderLineItemRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final OrderTransactionRepository orderTransactionRepository;
    private final AbandonedCheckoutRepository abandonedCheckoutRepository;
    private final com.shopai.rag.service.KnowledgeIndexService knowledgeIndexService;
    private final ObjectMapper objectMapper;

    public OrderSyncService(
            ShopifyClient shopifyClient,
            OrderRepository orderRepository,
            OrderLineItemRepository orderLineItemRepository,
            FulfillmentRepository fulfillmentRepository,
            OrderTransactionRepository orderTransactionRepository,
            AbandonedCheckoutRepository abandonedCheckoutRepository,
            com.shopai.rag.service.KnowledgeIndexService knowledgeIndexService,
            ObjectMapper objectMapper
    ) {
        this.shopifyClient = shopifyClient;
        this.orderRepository = orderRepository;
        this.orderLineItemRepository = orderLineItemRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderTransactionRepository = orderTransactionRepository;
        this.abandonedCheckoutRepository = abandonedCheckoutRepository;
        this.knowledgeIndexService = knowledgeIndexService;
        this.objectMapper = objectMapper;
    }

    private static final String ORDERS_QUERY = """
        query FetchOrders($first: Int!, $after: String) {
          orders(first: $first, after: $after, sortKey: CREATED_AT, reverse: true) {
            pageInfo {
              hasNextPage
              endCursor
            }
            edges {
              node {
                id
                name
                email
                phone
                displayFinancialStatus
                displayFulfillmentStatus
                currencyCode
                totalPriceSet { presentmentMoney { amount currencyCode } }
                subtotalPriceSet { presentmentMoney { amount currencyCode } }
                totalDiscountsSet { presentmentMoney { amount currencyCode } }
                totalTaxSet { presentmentMoney { amount currencyCode } }
                totalShippingPriceSet { presentmentMoney { amount currencyCode } }
                cancelledAt
                cancelReason
                customer {
                  id
                  firstName
                  lastName
                  email
                }
                shippingAddress {
                  address1
                  city
                  province
                  country
                  zip
                }
                billingAddress {
                  address1
                  city
                  province
                  country
                  zip
                }
                tags
                note
                createdAt
                updatedAt
                lineItems(first: 50) {
                  edges {
                    node {
                      id
                      title
                      variantTitle
                      sku
                      quantity
                      nonFulfillableQuantity
                      refundableQuantity
                      discountedUnitPriceAfterAllDiscountsSet { presentmentMoney { amount } }
                      requiresShipping
                      taxable
                    }
                  }
                }
                fulfillments {
                  id
                  status
                  trackingInfo {
                    company
                    number
                    url
                  }
                  service {
                    serviceName
                  }
                  createdAt
                  updatedAt
                }
                transactions {
                  id
                  kind
                  status
                  gateway
                  amountSet { presentmentMoney { amount currencyCode } }
                  formattedGateway
                  errorCode
                  processedAt
                  createdAt
                }
              }
            }
          }
        }
    """;

    @Transactional
    public int performFullOrderSync(SyncJob syncJob) {
        log.info("Starting full Shopify Orders & Fulfillments synchronization...");
        int totalProcessed = 0;
        String cursor = null;
        boolean hasNext = true;

        while (hasNext) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("first", 50);
            variables.put("after", cursor);

            com.shopai.shopify.client.GraphQLResponse response = shopifyClient.executeGraphQL(ORDERS_QUERY, variables);
            if (response.hasErrors() || response.data() == null) {
                log.warn("GraphQL errors in orders sync: {}", response.errors());
                break;
            }
            JsonNode ordersData = response.data().path("orders");
            JsonNode edges = ordersData.path("edges");

            if (edges.isArray()) {
                for (JsonNode edge : edges) {
                    JsonNode node = edge.path("node");
                    upsertOrderFromGraphQL(node);
                    totalProcessed++;
                }
            }

            JsonNode pageInfo = ordersData.path("pageInfo");
            hasNext = pageInfo.path("hasNextPage").asBoolean(false);
            cursor = pageInfo.path("endCursor").asText(null);

            if (syncJob != null) {
                syncJob.setItemsProcessed(totalProcessed);
            }
        }

        log.info("Completed Shopify Orders sync. Total orders processed: {}", totalProcessed);
        return totalProcessed;
    }

    @Transactional
    public void upsertOrderFromGraphQL(JsonNode node) {
        Long shopifyOrderId = parseGid(node.path("id").asText());
        if (shopifyOrderId == null) return;

        String orderName = node.path("name").asText("#" + shopifyOrderId);
        String orderNumber = orderName.replace("#", "");

        Order order = orderRepository.findByShopifyOrderId(shopifyOrderId)
                .orElseGet(() -> new Order(shopifyOrderId, orderNumber, orderName));

        order.setName(orderName);
        order.setOrderNumber(orderNumber);
        order.setEmail(node.path("email").asText(null));
        order.setPhone(node.path("phone").asText(null));
        order.setFinancialStatus(node.path("displayFinancialStatus").asText("PENDING"));
        order.setFulfillmentStatus(node.path("displayFulfillmentStatus").asText("UNFULFILLED"));
        order.setCurrency(node.path("currencyCode").asText("USD"));

        if (node.path("totalPriceSet").path("presentmentMoney").hasNonNull("amount")) {
            order.setTotalPrice(new BigDecimal(node.path("totalPriceSet").path("presentmentMoney").path("amount").asText("0.00")));
        }
        if (node.path("subtotalPriceSet").path("presentmentMoney").hasNonNull("amount")) {
            order.setSubtotalPrice(new BigDecimal(node.path("subtotalPriceSet").path("presentmentMoney").path("amount").asText("0.00")));
        }
        if (node.path("totalDiscountsSet").path("presentmentMoney").hasNonNull("amount")) {
            order.setTotalDiscounts(new BigDecimal(node.path("totalDiscountsSet").path("presentmentMoney").path("amount").asText("0.00")));
        }
        if (node.path("totalTaxSet").path("presentmentMoney").hasNonNull("amount")) {
            order.setTotalTax(new BigDecimal(node.path("totalTaxSet").path("presentmentMoney").path("amount").asText("0.00")));
        }
        if (node.path("totalShippingPriceSet").path("presentmentMoney").hasNonNull("amount")) {
            order.setTotalShipping(new BigDecimal(node.path("totalShippingPriceSet").path("presentmentMoney").path("amount").asText("0.00")));
        }

        if (node.hasNonNull("cancelledAt")) {
            order.setCancelledAt(Instant.parse(node.path("cancelledAt").asText()));
            order.setCancelReason(node.path("cancelReason").asText(null));
        }

        JsonNode customer = node.path("customer");
        if (!customer.isMissingNode() && !customer.isNull()) {
            order.setCustomerId(parseGid(customer.path("id").asText()));
            order.setCustomerFirstName(customer.path("firstName").asText(null));
            order.setCustomerLastName(customer.path("lastName").asText(null));
            order.setCustomerEmail(customer.path("email").asText(null));
        }

        if (!node.path("shippingAddress").isMissingNode()) {
            order.setShippingAddress(node.path("shippingAddress").toString());
        }
        if (!node.path("billingAddress").isMissingNode()) {
            order.setBillingAddress(node.path("billingAddress").toString());
        }

        if (node.path("tags").isArray()) {
            List<String> tagsList = new ArrayList<>();
            node.path("tags").forEach(t -> tagsList.add(t.asText()));
            order.setTags(tagsList.toArray(new String[0]));
        } else if (node.hasNonNull("tags")) {
            String tagsStr = node.path("tags").asText("");
            order.setTags(tagsStr.isEmpty() ? new String[0] : tagsStr.split("\\s*,\\s*"));
        } else {
            order.setTags(new String[0]);
        }

        order.setNote(node.path("note").asText(null));
        if (node.hasNonNull("createdAt")) {
            order.setShopifyCreatedAt(Instant.parse(node.path("createdAt").asText()));
        }
        if (node.hasNonNull("updatedAt")) {
            order.setShopifyUpdatedAt(Instant.parse(node.path("updatedAt").asText()));
        }
        order.setSyncedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        order = orderRepository.save(order);

        // Line Items
        JsonNode lineEdges = node.path("lineItems").path("edges");
        if (lineEdges.isArray()) {
            for (JsonNode lEdge : lineEdges) {
                JsonNode lNode = lEdge.path("node");
                Long lineId = parseGid(lNode.path("id").asText());
                if (lineId != null) {
                    OrderLineItem item = orderLineItemRepository.findByShopifyLineItemId(lineId)
                            .orElseGet(() -> new OrderLineItem(null, lineId, lNode.path("title").asText(), 1, BigDecimal.ZERO));

                    item.setOrder(order);
                    item.setTitle(lNode.path("title").asText());
                    item.setVariantTitle(lNode.path("variantTitle").asText(null));
                    item.setSku(lNode.path("sku").asText(null));
                    item.setQuantity(lNode.path("quantity").asInt(1));
                    if (lNode.path("discountedUnitPriceAfterAllDiscountsSet").path("presentmentMoney").hasNonNull("amount")) {
                        item.setPrice(new BigDecimal(lNode.path("discountedUnitPriceAfterAllDiscountsSet").path("presentmentMoney").path("amount").asText("0.00")));
                    }
                    item.setRequiresShipping(lNode.path("requiresShipping").asBoolean(true));
                    item.setTaxable(lNode.path("taxable").asBoolean(true));
                    orderLineItemRepository.save(item);
                }
            }
        }

        // Fulfillments
        JsonNode fulfillments = node.path("fulfillments");
        if (fulfillments.isArray()) {
            for (JsonNode fNode : fulfillments) {
                Long fId = parseGid(fNode.path("id").asText());
                if (fId != null) {
                    Fulfillment fulfillment = fulfillmentRepository.findByShopifyFulfillmentId(fId)
                            .orElseGet(() -> new Fulfillment(null, fId, fNode.path("status").asText("SUCCESS")));

                    fulfillment.setOrder(order);
                    fulfillment.setStatus(fNode.path("status").asText("SUCCESS"));

                    JsonNode tracking = fNode.path("trackingInfo");
                    if (tracking.isArray() && tracking.size() > 0) {
                        List<String> numbersList = new ArrayList<>();
                        List<String> urlsList = new ArrayList<>();
                        for (JsonNode tItem : tracking) {
                            if (tItem.hasNonNull("number")) numbersList.add(tItem.path("number").asText());
                            if (tItem.hasNonNull("url")) urlsList.add(tItem.path("url").asText());
                        }
                        JsonNode t0 = tracking.get(0);
                        fulfillment.setTrackingCompany(t0.path("company").asText(null));
                        fulfillment.setTrackingNumber(t0.path("number").asText(null));
                        fulfillment.setTrackingUrl(t0.path("url").asText(null));
                        fulfillment.setTrackingNumbers(numbersList.toArray(new String[0]));
                        fulfillment.setTrackingUrls(urlsList.toArray(new String[0]));
                    }

                    if (fNode.hasNonNull("createdAt")) {
                        fulfillment.setShopifyCreatedAt(Instant.parse(fNode.path("createdAt").asText()));
                    }
                    if (fNode.hasNonNull("updatedAt")) {
                        fulfillment.setShopifyUpdatedAt(Instant.parse(fNode.path("updatedAt").asText()));
                    }
                    fulfillmentRepository.save(fulfillment);
                }
            }
        }

        // Transactions
        JsonNode transactions = node.path("transactions");
        if (transactions.isArray()) {
            for (JsonNode tNode : transactions) {
                Long tId = parseGid(tNode.path("id").asText());
                if (tId != null) {
                    OrderTransaction transaction = orderTransactionRepository.findByShopifyTransactionId(tId)
                            .orElseGet(() -> new OrderTransaction(null, tId, tNode.path("kind").asText("SALE"), BigDecimal.ZERO, "USD"));

                    transaction.setOrder(order);
                    transaction.setKind(tNode.path("kind").asText("SALE"));
                    transaction.setStatus(tNode.path("status").asText("SUCCESS"));
                    transaction.setGateway(tNode.path("gateway").asText(null));
                    transaction.setPaymentMethodName(tNode.path("formattedGateway").asText(null));

                    if (tNode.path("amountSet").path("presentmentMoney").hasNonNull("amount")) {
                        transaction.setAmount(new BigDecimal(tNode.path("amountSet").path("presentmentMoney").path("amount").asText("0.00")));
                        transaction.setCurrency(tNode.path("amountSet").path("presentmentMoney").path("currencyCode").asText("USD"));
                    }
                    if (tNode.hasNonNull("processedAt")) {
                        transaction.setProcessedAt(Instant.parse(tNode.path("processedAt").asText()));
                    }
                    orderTransactionRepository.save(transaction);
                }
            }
        }
        knowledgeIndexService.indexOrder(order);
    }

    @Transactional
    public void processWebhookOrderUpsert(JsonNode payload) {
        Long shopifyOrderId = payload.path("id").asLong();
        if (shopifyOrderId == 0) return;

        String orderName = payload.path("name").asText("#" + shopifyOrderId);
        String orderNumber = payload.path("order_number").asText(orderName.replace("#", ""));

        Order order = orderRepository.findByShopifyOrderId(shopifyOrderId)
                .orElseGet(() -> new Order(shopifyOrderId, orderNumber, orderName));

        order.setName(orderName);
        order.setOrderNumber(orderNumber);
        order.setEmail(payload.path("email").asText(null));
        order.setPhone(payload.path("phone").asText(null));
        order.setFinancialStatus(payload.path("financial_status").asText("PENDING").toUpperCase());
        order.setFulfillmentStatus(payload.path("fulfillment_status").asText("UNFULFILLED").toUpperCase());
        order.setCurrency(payload.path("currency").asText("USD"));

        if (payload.hasNonNull("total_price")) {
            order.setTotalPrice(new BigDecimal(payload.path("total_price").asText("0.00")));
        }
        if (payload.hasNonNull("subtotal_price")) {
            order.setSubtotalPrice(new BigDecimal(payload.path("subtotal_price").asText("0.00")));
        }
        if (payload.hasNonNull("total_discounts")) {
            order.setTotalDiscounts(new BigDecimal(payload.path("total_discounts").asText("0.00")));
        }
        if (payload.hasNonNull("total_tax")) {
            order.setTotalTax(new BigDecimal(payload.path("total_tax").asText("0.00")));
        }

        if (payload.hasNonNull("cancelled_at")) {
            order.setCancelledAt(Instant.parse(payload.path("cancelled_at").asText()));
            order.setCancelReason(payload.path("cancel_reason").asText(null));
        }

        JsonNode customer = payload.path("customer");
        if (!customer.isMissingNode() && !customer.isNull()) {
            order.setCustomerId(customer.path("id").asLong());
            order.setCustomerFirstName(customer.path("first_name").asText(null));
            order.setCustomerLastName(customer.path("last_name").asText(null));
            order.setCustomerEmail(customer.path("email").asText(null));
        }

        if (!payload.path("shipping_address").isMissingNode()) {
            order.setShippingAddress(payload.path("shipping_address").toString());
        }
        if (!payload.path("billing_address").isMissingNode()) {
            order.setBillingAddress(payload.path("billing_address").toString());
        }

        if (payload.hasNonNull("tags")) {
            String tagsStr = payload.path("tags").asText("");
            order.setTags(tagsStr.isEmpty() ? new String[0] : tagsStr.split("\\s*,\\s*"));
        } else {
            order.setTags(new String[0]);
        }

        if (payload.hasNonNull("created_at")) {
            order.setShopifyCreatedAt(Instant.parse(payload.path("created_at").asText()));
        }
        if (payload.hasNonNull("updated_at")) {
            order.setShopifyUpdatedAt(Instant.parse(payload.path("updated_at").asText()));
        }
        order.setSyncedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        order = orderRepository.save(order);

        // Ingest Line Items from Webhook
        JsonNode lineItems = payload.path("line_items");
        if (lineItems.isArray()) {
            for (JsonNode lNode : lineItems) {
                Long lineId = lNode.path("id").asLong();
                if (lineId != 0) {
                    OrderLineItem item = orderLineItemRepository.findByShopifyLineItemId(lineId)
                            .orElseGet(() -> new OrderLineItem(null, lineId, lNode.path("title").asText(), 1, BigDecimal.ZERO));

                    item.setOrder(order);
                    item.setTitle(lNode.path("title").asText());
                    item.setVariantTitle(lNode.path("variant_title").asText(null));
                    item.setSku(lNode.path("sku").asText(null));
                    item.setQuantity(lNode.path("quantity").asInt(1));
                    item.setFulfillableQuantity(lNode.path("fulfillable_quantity").asInt(0));
                    if (lNode.hasNonNull("price")) {
                        item.setPrice(new BigDecimal(lNode.path("price").asText("0.00")));
                    }
                    orderLineItemRepository.save(item);
                }
            }
        }

        // Ingest Fulfillments from Webhook
        JsonNode fulfillments = payload.path("fulfillments");
        if (fulfillments.isArray()) {
            for (JsonNode fNode : fulfillments) {
                Long fId = fNode.path("id").asLong();
                if (fId != 0) {
                    Fulfillment fulfillment = fulfillmentRepository.findByShopifyFulfillmentId(fId)
                            .orElseGet(() -> new Fulfillment(null, fId, fNode.path("status").asText("SUCCESS")));

                    fulfillment.setOrder(order);
                    fulfillment.setStatus(fNode.path("status").asText("SUCCESS").toUpperCase());
                    fulfillment.setTrackingCompany(fNode.path("tracking_company").asText(null));
                    fulfillment.setTrackingNumber(fNode.path("tracking_number").asText(null));
                    fulfillment.setTrackingUrl(fNode.path("tracking_url").asText(null));
                    if (fNode.path("tracking_numbers").isArray()) {
                        List<String> nums = new ArrayList<>();
                        fNode.path("tracking_numbers").forEach(n -> nums.add(n.asText()));
                        fulfillment.setTrackingNumbers(nums.toArray(new String[0]));
                    } else if (fulfillment.getTrackingNumber() != null) {
                        fulfillment.setTrackingNumbers(new String[]{fulfillment.getTrackingNumber()});
                    }
                    if (fNode.path("tracking_urls").isArray()) {
                        List<String> uList = new ArrayList<>();
                        fNode.path("tracking_urls").forEach(u -> uList.add(u.asText()));
                        fulfillment.setTrackingUrls(uList.toArray(new String[0]));
                    } else if (fulfillment.getTrackingUrl() != null) {
                        fulfillment.setTrackingUrls(new String[]{fulfillment.getTrackingUrl()});
                    }
                    fulfillmentRepository.save(fulfillment);
                }
            }
        }

        knowledgeIndexService.indexOrder(order);
        log.info("Processed webhook upsert for order: {}", order.getName());
    }

    @Transactional
    public void processWebhookCheckoutUpsert(JsonNode payload) {
        Long checkoutId = payload.path("id").asLong();
        if (checkoutId == 0) return;

        AbandonedCheckout checkout = abandonedCheckoutRepository.findByShopifyCheckoutId(checkoutId)
                .orElseGet(() -> new AbandonedCheckout(checkoutId, payload.path("email").asText(null), BigDecimal.ZERO));

        checkout.setCartToken(payload.path("cart_token").asText(null));
        checkout.setEmail(payload.path("email").asText(null));
        checkout.setPhone(payload.path("phone").asText(null));

        JsonNode customer = payload.path("customer");
        if (!customer.isMissingNode() && !customer.isNull()) {
            checkout.setCustomerName(customer.path("first_name").asText("") + " " + customer.path("last_name").asText(""));
        }

        if (payload.hasNonNull("subtotal_price")) {
            checkout.setSubtotalPrice(new BigDecimal(payload.path("subtotal_price").asText("0.00")));
        }
        if (payload.hasNonNull("total_price")) {
            checkout.setTotalPrice(new BigDecimal(payload.path("total_price").asText("0.00")));
        }
        checkout.setCurrency(payload.path("currency").asText("USD"));
        checkout.setAbandonedCheckoutUrl(payload.path("abandoned_checkout_url").asText(null));

        if (payload.hasNonNull("completed_at")) {
            checkout.setCompletedAt(Instant.parse(payload.path("completed_at").asText()));
            checkout.setRecoveryStatus("RECOVERED");
        } else {
            checkout.setRecoveryStatus("ABANDONED");
        }

        if (!payload.path("line_items").isMissingNode()) {
            checkout.setLineItems(payload.path("line_items").toString());
        }

        if (payload.hasNonNull("created_at")) {
            checkout.setShopifyCreatedAt(Instant.parse(payload.path("created_at").asText()));
        }
        if (payload.hasNonNull("updated_at")) {
            checkout.setShopifyUpdatedAt(Instant.parse(payload.path("updated_at").asText()));
        }
        checkout.setSyncedAt(Instant.now());
        checkout.setUpdatedAt(Instant.now());

        checkout = abandonedCheckoutRepository.save(checkout);
        knowledgeIndexService.indexAbandonedCheckout(checkout);
        log.info("Processed webhook upsert for checkout: {}", checkoutId);
    }

    private Long parseGid(String gid) {
        if (gid == null || !gid.contains("/")) return null;
        try {
            String[] parts = gid.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }
}
