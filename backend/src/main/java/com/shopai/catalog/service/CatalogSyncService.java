package com.shopai.catalog.service;

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
    private final com.shopai.rag.service.KnowledgeIndexService knowledgeIndexService;
    private final ObjectMapper objectMapper;

    public CatalogSyncService(
            ShopifyClient shopifyClient,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CollectionRepository collectionRepository,
            com.shopai.rag.service.KnowledgeIndexService knowledgeIndexService,
            ObjectMapper objectMapper
    ) {
        this.shopifyClient = shopifyClient;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.collectionRepository = collectionRepository;
        this.knowledgeIndexService = knowledgeIndexService;
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

            com.shopai.shopify.client.GraphQLResponse response = shopifyClient.executeGraphQL(PRODUCTS_QUERY, variables);
            if (response.hasErrors() || response.data() == null) {
                log.warn("GraphQL errors in catalog sync: {}", response.errors());
                break;
            }
            JsonNode productsData = response.data().path("products");
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
            product.setTags(tagsList.toArray(new String[0]));
        } else if (node.hasNonNull("tags")) {
            String tagsStr = node.path("tags").asText("");
            product.setTags(tagsStr.isEmpty() ? new String[0] : tagsStr.split("\\s*,\\s*"));
        } else {
            product.setTags(new String[0]);
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
        knowledgeIndexService.indexProduct(product);
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
        if (payload.hasNonNull("tags")) {
            String tagsStr = payload.path("tags").asText("");
            product.setTags(tagsStr.isEmpty() ? new String[0] : tagsStr.split("\\s*,\\s*"));
        } else {
            product.setTags(new String[0]);
        }

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
        product = productRepository.save(product);
        knowledgeIndexService.indexProduct(product);
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
