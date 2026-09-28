import os

def write_file(rel_path, content):
    full_path = os.path.join(r'c:\apps\ShopAI\backend\src\main\java\com\shopai', rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print('Created:', rel_path)

# ==============================================================================
# CATALOG SYNC SERVICE
# ==============================================================================

write_file('catalog/service/CatalogSyncService.java', '''package com.shopai.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.catalog.domain.Collection;
import com.shopai.catalog.domain.Product;
import com.shopai.catalog.domain.ProductVariant;
import com.shopai.catalog.repository.CollectionRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.catalog.repository.ProductVariantRepository;
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
public class CatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncService.class);

    private final ShopifyClient shopifyClient;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CollectionRepository collectionRepository;
    private final ObjectMapper objectMapper;

    public CatalogSyncService(
            ShopifyClient shopifyClient,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CollectionRepository collectionRepository,
            ObjectMapper objectMapper
    ) {
        this.shopifyClient = shopifyClient;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.collectionRepository = collectionRepository;
        this.objectMapper = objectMapper;
    }

    private static final String PRODUCTS_QUERY = """
        query FetchProducts($first: Int!, $after: String) {
          products(first: $first, after: $after) {
            pageInfo {
              hasNextPage
              endCursor
            }
            edges {
              node {
                id
                title
                handle
                descriptionHtml
                vendor
                productType
                status
                tags
                totalInventory
                publishedAt
                createdAt
                updatedAt
                variants(first: 50) {
                  edges {
                    node {
                      id
                      title
                      sku
                      barcode
                      price
                      compareAtPrice
                      inventoryQuantity
                      position
                      image {
                        url
                      }
                    }
                  }
                }
                collections(first: 20) {
                  edges {
                    node {
                      id
                      title
                      handle
                      description
                    }
                  }
                }
              }
            }
          }
        }
    """;

    @Transactional
    public int performFullCatalogSync(SyncJob syncJob) {
        log.info("Starting full Shopify Catalog synchronization...");
        int totalProcessed = 0;
        String cursor = null;
        boolean hasNext = true;

        while (hasNext) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("first", 50);
            variables.put("after", cursor);

            JsonNode response = shopifyClient.executeGraphQL(PRODUCTS_QUERY, variables);
            JsonNode productsData = response.path("data").path("products");
            JsonNode edges = productsData.path("edges");

            if (edges.isArray()) {
                for (JsonNode edge : edges) {
                    JsonNode node = edge.path("node");
                    upsertProductFromGraphQL(node);
                    totalProcessed++;
                }
            }

            JsonNode pageInfo = productsData.path("pageInfo");
            hasNext = pageInfo.path("hasNextPage").asBoolean(false);
            cursor = pageInfo.path("endCursor").asText(null);

            if (syncJob != null) {
                syncJob.setItemsProcessed(totalProcessed);
            }
        }

        log.info("Completed Shopify Catalog sync. Total products processed: {}", totalProcessed);
        return totalProcessed;
    }

    @Transactional
    public void upsertProductFromGraphQL(JsonNode node) {
        Long shopifyProductId = parseGid(node.path("id").asText());
        if (shopifyProductId == null) return;

        Product product = productRepository.findByShopifyProductId(shopifyProductId)
                .orElseGet(() -> new Product(shopifyProductId, node.path("title").asText(), node.path("handle").asText()));

        product.setTitle(node.path("title").asText("Untitled"));
        product.setHandle(node.path("handle").asText(""));
        product.setDescription(node.path("descriptionHtml").asText(""));
        product.setVendor(node.path("vendor").asText(""));
        product.setProductType(node.path("productType").asText(""));
        product.setStatus(node.path("status").asText("ACTIVE"));
        product.setTotalInventory(node.path("totalInventory").asInt(0));

        if (node.hasNonNull("publishedAt")) {
            product.setPublishedAt(Instant.parse(node.path("publishedAt").asText()));
        }
        if (node.hasNonNull("createdAt")) {
            product.setShopifyCreatedAt(Instant.parse(node.path("createdAt").asText()));
        }
        if (node.hasNonNull("updatedAt")) {
            product.setShopifyUpdatedAt(Instant.parse(node.path("updatedAt").asText()));
        }
        product.setSyncedAt(Instant.now());
        product.setUpdatedAt(Instant.now());

        if (node.path("tags").isArray()) {
            List<String> tagsList = new ArrayList<>();
            node.path("tags").forEach(t -> tagsList.add(t.asText()));
            product.setTags(String.join(",", tagsList));
        } else {
            product.setTags(node.path("tags").asText(""));
        }

        // Collections mapping
        JsonNode collEdges = node.path("collections").path("edges");
        Set<Collection> collections = new HashSet<>();
        if (collEdges.isArray()) {
            for (JsonNode cEdge : collEdges) {
                JsonNode cNode = cEdge.path("node");
                Long cId = parseGid(cNode.path("id").asText());
                if (cId != null) {
                    Collection coll = collectionRepository.findByShopifyCollectionId(cId)
                            .orElseGet(() -> {
                                Collection newC = new Collection(cId, cNode.path("title").asText(), cNode.path("handle").asText());
                                newC.setDescription(cNode.path("description").asText(""));
                                return collectionRepository.save(newC);
                            });
                    collections.add(coll);
                }
            }
        }
        product.setCollections(collections);
        product = productRepository.save(product);

        // Variants mapping
        JsonNode varEdges = node.path("variants").path("edges");
        if (varEdges.isArray()) {
            for (JsonNode vEdge : varEdges) {
                JsonNode vNode = vEdge.path("node");
                Long vId = parseGid(vNode.path("id").asText());
                if (vId != null) {
                    ProductVariant variant = productVariantRepository.findByShopifyVariantId(vId)
                            .orElseGet(() -> new ProductVariant(null, vId, vNode.path("title").asText(), BigDecimal.ZERO));

                    variant.setProduct(product);
                    variant.setTitle(vNode.path("title").asText("Default Title"));
                    variant.setSku(vNode.path("sku").asText(""));
                    variant.setBarcode(vNode.path("barcode").asText(""));

                    if (vNode.hasNonNull("price")) {
                        variant.setPrice(new BigDecimal(vNode.path("price").asText("0.00")));
                    }
                    if (vNode.hasNonNull("compareAtPrice")) {
                        variant.setCompareAtPrice(new BigDecimal(vNode.path("compareAtPrice").asText("0.00")));
                    }
                    variant.setInventoryQuantity(vNode.path("inventoryQuantity").asInt(0));
                    variant.setPosition(vNode.path("position").asInt(1));

                    if (vNode.path("image").hasNonNull("url")) {
                        variant.setImageUrl(vNode.path("image").path("url").asText());
                    }
                    variant.setUpdatedAt(Instant.now());
                    productVariantRepository.save(variant);
                }
            }
        }
    }

    @Transactional
    public void processWebhookProductUpsert(JsonNode payload) {
        Long shopifyProductId = payload.path("id").asLong();
        if (shopifyProductId == 0) return;

        Product product = productRepository.findByShopifyProductId(shopifyProductId)
                .orElseGet(() -> new Product(shopifyProductId, payload.path("title").asText(), payload.path("handle").asText()));

        product.setTitle(payload.path("title").asText("Untitled"));
        product.setHandle(payload.path("handle").asText(""));
        product.setDescription(payload.path("body_html").asText(""));
        product.setVendor(payload.path("vendor").asText(""));
        product.setProductType(payload.path("product_type").asText(""));
        product.setStatus(payload.path("status").asText("ACTIVE").toUpperCase());
        product.setTags(payload.path("tags").asText(""));

        if (payload.hasNonNull("published_at")) {
            product.setPublishedAt(Instant.parse(payload.path("published_at").asText()));
        }
        if (payload.hasNonNull("created_at")) {
            product.setShopifyCreatedAt(Instant.parse(payload.path("created_at").asText()));
        }
        if (payload.hasNonNull("updated_at")) {
            product.setShopifyUpdatedAt(Instant.parse(payload.path("updated_at").asText()));
        }
        product.setSyncedAt(Instant.now());
        product.setUpdatedAt(Instant.now());

        product = productRepository.save(product);

        // Process webhook variants
        JsonNode variantsArray = payload.path("variants");
        int totalInv = 0;
        if (variantsArray.isArray()) {
            for (JsonNode vNode : variantsArray) {
                Long vId = vNode.path("id").asLong();
                if (vId != 0) {
                    ProductVariant variant = productVariantRepository.findByShopifyVariantId(vId)
                            .orElseGet(() -> new ProductVariant(null, vId, vNode.path("title").asText(), BigDecimal.ZERO));

                    variant.setProduct(product);
                    variant.setTitle(vNode.path("title").asText("Default"));
                    variant.setSku(vNode.path("sku").asText(""));
                    variant.setBarcode(vNode.path("barcode").asText(""));
                    if (vNode.hasNonNull("price")) {
                        variant.setPrice(new BigDecimal(vNode.path("price").asText("0.00")));
                    }
                    if (vNode.hasNonNull("compare_at_price")) {
                        variant.setCompareAtPrice(new BigDecimal(vNode.path("compare_at_price").asText("0.00")));
                    }
                    int qty = vNode.path("inventory_quantity").asInt(0);
                    variant.setInventoryQuantity(qty);
                    totalInv += qty;
                    variant.setPosition(vNode.path("position").asInt(1));
                    variant.setRequiresShipping(vNode.path("requires_shipping").asBoolean(true));
                    variant.setUpdatedAt(Instant.now());
                    productVariantRepository.save(variant);
                }
            }
        }
        product.setTotalInventory(totalInv);
        productRepository.save(product);
        log.info("Processed webhook upsert for product: {}", product.getTitle());
    }

    @Transactional
    public void processWebhookProductDelete(JsonNode payload) {
        Long shopifyProductId = payload.path("id").asLong();
        if (shopifyProductId != 0) {
            productRepository.findByShopifyProductId(shopifyProductId).ifPresent(p -> {
                productRepository.delete(p);
                log.info("Deleted product from database: {}", shopifyProductId);
            });
        }
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
''')

# ==============================================================================
# ORDER SYNC SERVICE
# ==============================================================================

write_file('order/service/OrderSyncService.java', '''package com.shopai.order.service;

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
    private final ObjectMapper objectMapper;

    public OrderSyncService(
            ShopifyClient shopifyClient,
            OrderRepository orderRepository,
            OrderLineItemRepository orderLineItemRepository,
            FulfillmentRepository fulfillmentRepository,
            OrderTransactionRepository orderTransactionRepository,
            AbandonedCheckoutRepository abandonedCheckoutRepository,
            ObjectMapper objectMapper
    ) {
        this.shopifyClient = shopifyClient;
        this.orderRepository = orderRepository;
        this.orderLineItemRepository = orderLineItemRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderTransactionRepository = orderTransactionRepository;
        this.abandonedCheckoutRepository = abandonedCheckoutRepository;
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

            JsonNode response = shopifyClient.executeGraphQL(ORDERS_QUERY, variables);
            JsonNode ordersData = response.path("data").path("orders");
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
                        JsonNode t0 = tracking.get(0);
                        fulfillment.setTrackingCompany(t0.path("company").asText(null));
                        fulfillment.setTrackingNumber(t0.path("number").asText(null));
                        fulfillment.setTrackingUrl(t0.path("url").asText(null));
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
                    fulfillmentRepository.save(fulfillment);
                }
            }
        }

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

        abandonedCheckoutRepository.save(checkout);
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
''')

# ==============================================================================
# DATA SYNC SERVICE & CONTROLLER
# ==============================================================================

write_file('sync/service/DataSyncService.java', '''package com.shopai.sync.service;

import com.shopai.catalog.service.CatalogSyncService;
import com.shopai.order.service.OrderSyncService;
import com.shopai.sync.domain.SyncJob;
import com.shopai.sync.dto.SyncStatusResponse;
import com.shopai.sync.repository.SyncJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final CatalogSyncService catalogSyncService;
    private final OrderSyncService orderSyncService;
    private final SyncJobRepository syncJobRepository;

    public DataSyncService(
            CatalogSyncService catalogSyncService,
            OrderSyncService orderSyncService,
            SyncJobRepository syncJobRepository
    ) {
        this.catalogSyncService = catalogSyncService;
        this.orderSyncService = orderSyncService;
        this.syncJobRepository = syncJobRepository;
    }

    public SyncJob triggerSync(String jobType) {
        SyncJob job = new SyncJob(jobType.toUpperCase());
        job.setStatus("IN_PROGRESS");
        job.setStartedAt(Instant.now());
        job = syncJobRepository.save(job);

        runSyncAsync(job.getId(), job.getJobType());
        return job;
    }

    @Async
    public CompletableFuture<Void> runSyncAsync(java.util.UUID jobId, String jobType) {
        try {
            SyncJob job = syncJobRepository.findById(jobId).orElseThrow();
            int count = 0;
            if ("CATALOG_SYNC".equalsIgnoreCase(jobType)) {
                count = catalogSyncService.performFullCatalogSync(job);
            } else if ("ORDER_SYNC".equalsIgnoreCase(jobType)) {
                count = orderSyncService.performFullOrderSync(job);
            }
            job.setItemsProcessed(count);
            job.setStatus("COMPLETED");
            job.setCompletedAt(Instant.now());
            syncJobRepository.save(job);
            log.info("Sync job {} ({}) completed successfully. Processed: {}", jobId, jobType, count);
        } catch (Exception e) {
            log.error("Sync job {} ({}) failed: {}", jobId, jobType, e.getMessage(), e);
            syncJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus("FAILED");
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(Instant.now());
                syncJobRepository.save(job);
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    public SyncStatusResponse getStatus(String jobType) {
        SyncJob current = (jobType != null && !jobType.isBlank())
                ? syncJobRepository.findTopByJobTypeOrderByStartedAtDesc(jobType.toUpperCase()).orElse(null)
                : syncJobRepository.findTop10ByOrderByStartedAtDesc().stream().findFirst().orElse(null);

        List<SyncJob> recent = syncJobRepository.findTop10ByOrderByStartedAtDesc();
        return SyncStatusResponse.from(current, recent);
    }
}
''')

write_file('sync/controller/DataSyncController.java', '''package com.shopai.sync.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.sync.domain.SyncJob;
import com.shopai.sync.dto.SyncStatusResponse;
import com.shopai.sync.service.DataSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
public class DataSyncController {

    private final DataSyncService dataSyncService;

    public DataSyncController(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @PostMapping("/catalog")
    @PreAuthorize("hasAuthority('integration.manage')")
    public ResponseEntity<ApiResponse<SyncJob>> triggerCatalogSync() {
        SyncJob job = dataSyncService.triggerSync("CATALOG_SYNC");
        return ResponseEntity.ok(ApiResponse.ok("Catalog synchronization initiated", job));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('integration.manage')")
    public ResponseEntity<ApiResponse<SyncJob>> triggerOrderSync() {
        SyncJob job = dataSyncService.triggerSync("ORDER_SYNC");
        return ResponseEntity.ok(ApiResponse.ok("Order synchronization initiated", job));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('integration.manage', 'product.read', 'order.read')")
    public ResponseEntity<ApiResponse<SyncStatusResponse>> getSyncStatus(@RequestParam(required = false) String jobType) {
        SyncStatusResponse status = dataSyncService.getStatus(jobType);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
''')

print('Sync services generated successfully.')

