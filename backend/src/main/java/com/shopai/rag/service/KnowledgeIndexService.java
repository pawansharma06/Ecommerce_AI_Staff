package com.shopai.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.catalog.domain.Product;
import com.shopai.catalog.domain.ProductVariant;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.order.domain.AbandonedCheckout;
import com.shopai.order.domain.Fulfillment;
import com.shopai.order.domain.Order;
import com.shopai.order.domain.OrderLineItem;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.order.repository.OrderRepository;
import com.shopai.rag.domain.KnowledgeDocument;
import com.shopai.rag.embedding.EmbeddingService;
import com.shopai.rag.repository.KnowledgeChunkRepository;
import com.shopai.rag.repository.KnowledgeDocumentRepository;
import com.shopai.sync.domain.SyncJob;
import com.shopai.sync.repository.SyncJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class KnowledgeIndexService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AbandonedCheckoutRepository abandonedCheckoutRepository;
    private final SyncJobRepository syncJobRepository;
    private final TextChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public KnowledgeIndexService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkRepository chunkRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            AbandonedCheckoutRepository abandonedCheckoutRepository,
            SyncJobRepository syncJobRepository,
            TextChunkingService chunkingService,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.abandonedCheckoutRepository = abandonedCheckoutRepository;
        this.syncJobRepository = syncJobRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void indexProduct(Product product) {
        if (product == null) return;

        String sourceId = String.valueOf(product.getShopifyProductId());
        KnowledgeDocument doc = documentRepository.findByDocumentTypeAndSourceId("PRODUCT", sourceId)
                .orElseGet(() -> new KnowledgeDocument(product.getTitle(), "", "PRODUCT", sourceId));

        doc.setTitle(product.getTitle());

        StringBuilder sb = new StringBuilder();
        sb.append("PRODUCT: ").append(product.getTitle()).append("\n");
        if (product.getVendor() != null && !product.getVendor().isBlank()) {
            sb.append("Vendor: ").append(product.getVendor()).append("\n");
        }
        if (product.getProductType() != null && !product.getProductType().isBlank()) {
            sb.append("Product Type: ").append(product.getProductType()).append("\n");
        }
        sb.append("Status: ").append(product.getStatus()).append("\n");
        sb.append("Total Inventory: ").append(product.getTotalInventory()).append("\n");

        if (product.getTags() != null && product.getTags().length > 0) {
            sb.append("Tags: ").append(String.join(", ", product.getTags())).append("\n");
        }

        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            String cleanDesc = product.getDescription().replaceAll("<[^>]*>", "").trim();
            sb.append("Description:\n").append(cleanDesc).append("\n\n");
        }

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            sb.append("Variants & Pricing:\n");
            for (ProductVariant v : product.getVariants()) {
                sb.append("- ").append(v.getTitle())
                        .append(" | SKU: ").append(v.getSku() != null ? v.getSku() : "N/A")
                        .append(" | Price: $").append(v.getPrice() != null ? v.getPrice() : "0.00")
                        .append(" | In Stock: ").append(v.getInventoryQuantity())
                        .append("\n");
            }
        }

        String fullContent = sb.toString();
        doc.setContent(fullContent);
        doc.setStatus("INDEXED");
        doc.setUpdatedAt(Instant.now());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", product.getId().toString());
        metadata.put("shopifyProductId", product.getShopifyProductId());
        metadata.put("handle", product.getHandle());
        metadata.put("vendor", product.getVendor());
        metadata.put("document_type", "PRODUCT");
        try {
            doc.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            doc.setMetadata("{}");
        }

        doc = documentRepository.save(doc);

        // Re-generate and embed chunks
        chunkRepository.deleteByDocumentId(doc.getId());
        List<TextChunkingService.Chunk> chunks = chunkingService.chunkText(fullContent);

        for (TextChunkingService.Chunk chunk : chunks) {
            UUID chunkId = UUID.randomUUID();
            String embeddingStr = embeddingService.getEmbeddingAsVectorString(chunk.content());
            chunkRepository.insertChunkWithEmbedding(
                    chunkId,
                    doc.getId(),
                    chunk.index(),
                    chunk.content(),
                    chunk.tokenCount(),
                    embeddingStr,
                    null,
                    null,
                    doc.getMetadata()
            );
        }
    }

    @Transactional
    public void indexOrder(Order order) {
        if (order == null || order.getEmail() == null && order.getCustomerEmail() == null) return;

        String customerEmail = order.getCustomerEmail() != null ? order.getCustomerEmail() : order.getEmail();
        String sourceId = String.valueOf(order.getShopifyOrderId());

        KnowledgeDocument doc = documentRepository.findByDocumentTypeAndSourceId("ORDER_HISTORY", sourceId)
                .orElseGet(() -> new KnowledgeDocument("Order " + order.getName(), "", "ORDER_HISTORY", sourceId));

        doc.setTitle("Order " + order.getName() + " (" + order.getFinancialStatus() + ")");
        doc.setCustomerId(order.getCustomerId());
        doc.setCustomerEmail(customerEmail);

        StringBuilder sb = new StringBuilder();
        sb.append("ORDER: ").append(order.getName()).append(" (#").append(order.getOrderNumber()).append(")\n");
        sb.append("Customer: ").append(order.getCustomerFirstName() != null ? order.getCustomerFirstName() : "").append(" ")
                .append(order.getCustomerLastName() != null ? order.getCustomerLastName() : "").append(" (").append(customerEmail).append(")\n");
        sb.append("Financial Status: ").append(order.getFinancialStatus()).append("\n");
        sb.append("Fulfillment Status: ").append(order.getFulfillmentStatus()).append("\n");
        sb.append("Order Total: $").append(order.getTotalPrice() != null ? order.getTotalPrice() : "0.00").append(" ").append(order.getCurrency()).append("\n");
        sb.append("Date Placed: ").append(order.getShopifyCreatedAt() != null ? order.getShopifyCreatedAt().toString() : "N/A").append("\n\n");

        if (order.getShippingAddress() != null) {
            sb.append("Shipping Address: ").append(order.getShippingAddress()).append("\n");
        }

        if (order.getLineItems() != null && !order.getLineItems().isEmpty()) {
            sb.append("Ordered Items:\n");
            for (OrderLineItem item : order.getLineItems()) {
                sb.append("- ").append(item.getTitle())
                        .append(item.getVariantTitle() != null ? " (" + item.getVariantTitle() + ")" : "")
                        .append(" | Qty: ").append(item.getQuantity())
                        .append(" | Price: $").append(item.getPrice())
                        .append("\n");
            }
            sb.append("\n");
        }

        if (order.getFulfillments() != null && !order.getFulfillments().isEmpty()) {
            sb.append("Shipments & Tracking Updates:\n");
            for (Fulfillment f : order.getFulfillments()) {
                sb.append("- Shipment Status: ").append(f.getStatus())
                        .append(" | Carrier: ").append(f.getTrackingCompany() != null ? f.getTrackingCompany() : "Standard")
                        .append(" | Tracking #: ").append(f.getTrackingNumber() != null ? f.getTrackingNumber() : "None")
                        .append(f.getTrackingUrl() != null ? " | URL: " + f.getTrackingUrl() : "")
                        .append("\n");
            }
        }

        String fullContent = sb.toString();
        doc.setContent(fullContent);
        doc.setStatus("INDEXED");
        doc.setUpdatedAt(Instant.now());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderId", order.getId().toString());
        metadata.put("shopifyOrderId", order.getShopifyOrderId());
        metadata.put("orderNumber", order.getOrderNumber());
        metadata.put("customerEmail", customerEmail);
        metadata.put("document_type", "ORDER_HISTORY");
        try {
            doc.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            doc.setMetadata("{}");
        }

        doc = documentRepository.save(doc);

        chunkRepository.deleteByDocumentId(doc.getId());
        List<TextChunkingService.Chunk> chunks = chunkingService.chunkText(fullContent);

        for (TextChunkingService.Chunk chunk : chunks) {
            UUID chunkId = UUID.randomUUID();
            String embeddingStr = embeddingService.getEmbeddingAsVectorString(chunk.content());
            chunkRepository.insertChunkWithEmbedding(
                    chunkId,
                    doc.getId(),
                    chunk.index(),
                    chunk.content(),
                    chunk.tokenCount(),
                    embeddingStr,
                    order.getCustomerId(),
                    customerEmail,
                    doc.getMetadata()
            );
        }
    }

    @Transactional
    public void indexAbandonedCheckout(AbandonedCheckout checkout) {
        if (checkout == null || checkout.getEmail() == null) return;

        String customerEmail = checkout.getEmail();
        String sourceId = String.valueOf(checkout.getShopifyCheckoutId());

        KnowledgeDocument doc = documentRepository.findByDocumentTypeAndSourceId("ABANDONED_CHECKOUT", sourceId)
                .orElseGet(() -> new KnowledgeDocument("Abandoned Cart (" + customerEmail + ")", "", "ABANDONED_CHECKOUT", sourceId));

        doc.setTitle("Abandoned Checkout Cart - " + customerEmail);
        doc.setCustomerEmail(customerEmail);

        StringBuilder sb = new StringBuilder();
        sb.append("ABANDONED CHECKOUT CART:\n");
        sb.append("Customer: ").append(checkout.getCustomerName() != null ? checkout.getCustomerName() : "Guest").append(" (").append(customerEmail).append(")\n");
        sb.append("Recovery Status: ").append(checkout.getRecoveryStatus()).append("\n");
        sb.append("Total Cart Value: $").append(checkout.getTotalPrice() != null ? checkout.getTotalPrice() : "0.00").append(" ").append(checkout.getCurrency()).append("\n");
        if (checkout.getAbandonedCheckoutUrl() != null) {
            sb.append("Recovery URL: ").append(checkout.getAbandonedCheckoutUrl()).append("\n");
        }
        if (checkout.getLineItems() != null) {
            sb.append("Cart Line Items: ").append(checkout.getLineItems()).append("\n");
        }

        String fullContent = sb.toString();
        doc.setContent(fullContent);
        doc.setStatus("INDEXED");
        doc.setUpdatedAt(Instant.now());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("checkoutId", checkout.getId().toString());
        metadata.put("shopifyCheckoutId", checkout.getShopifyCheckoutId());
        metadata.put("customerEmail", customerEmail);
        metadata.put("recoveryStatus", checkout.getRecoveryStatus());
        metadata.put("recoveryUrl", checkout.getAbandonedCheckoutUrl());
        metadata.put("document_type", "ABANDONED_CHECKOUT");
        try {
            doc.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            doc.setMetadata("{}");
        }

        doc = documentRepository.save(doc);

        chunkRepository.deleteByDocumentId(doc.getId());
        List<TextChunkingService.Chunk> chunks = chunkingService.chunkText(fullContent);

        for (TextChunkingService.Chunk chunk : chunks) {
            UUID chunkId = UUID.randomUUID();
            String embeddingStr = embeddingService.getEmbeddingAsVectorString(chunk.content());
            chunkRepository.insertChunkWithEmbedding(
                    chunkId,
                    doc.getId(),
                    chunk.index(),
                    chunk.content(),
                    chunk.tokenCount(),
                    embeddingStr,
                    null,
                    customerEmail,
                    doc.getMetadata()
            );
        }
    }

    @Transactional
    public void indexCustomDocument(KnowledgeDocument doc) {
        if (doc == null || doc.getContent() == null || doc.getContent().isBlank()) return;

        doc.setStatus("INDEXED");
        doc.setUpdatedAt(Instant.now());
        doc = documentRepository.save(doc);

        chunkRepository.deleteByDocumentId(doc.getId());
        List<TextChunkingService.Chunk> chunks = chunkingService.chunkText(doc.getContent());

        for (TextChunkingService.Chunk chunk : chunks) {
            UUID chunkId = UUID.randomUUID();
            String embeddingStr = embeddingService.getEmbeddingAsVectorString(chunk.content());
            chunkRepository.insertChunkWithEmbedding(
                    chunkId,
                    doc.getId(),
                    chunk.index(),
                    chunk.content(),
                    chunk.tokenCount(),
                    embeddingStr,
                    doc.getCustomerId(),
                    doc.getCustomerEmail(),
                    doc.getMetadata()
            );
        }
    }

    @Async
    public void reindexAllKnowledgeAsync() {
        log.info("Starting background full vector re-indexing...");
        SyncJob syncJob = new SyncJob("VECTOR_RAG_INDEX");
        syncJob = syncJobRepository.save(syncJob);

        int totalCount = 0;
        try {
            // 1. Index All Products
            List<Product> products = productRepository.findAll();
            for (Product p : products) {
                indexProduct(p);
                totalCount++;
            }

            // 2. Index All Orders
            List<Order> orders = orderRepository.findAll();
            for (Order o : orders) {
                indexOrder(o);
                totalCount++;
            }

            // 3. Index All Abandoned Checkouts
            List<AbandonedCheckout> checkouts = abandonedCheckoutRepository.findAll();
            for (AbandonedCheckout c : checkouts) {
                indexAbandonedCheckout(c);
                totalCount++;
            }

            // 4. Re-chunk custom documents
            List<KnowledgeDocument> customDocs = documentRepository.findAll().stream()
                    .filter(d -> "POLICY".equals(d.getDocumentType()) || "FAQ".equals(d.getDocumentType()) || "CUSTOM".equals(d.getDocumentType()))
                    .toList();
            for (KnowledgeDocument d : customDocs) {
                indexCustomDocument(d);
                totalCount++;
            }

            syncJob.setStatus("COMPLETED");
            syncJob.setItemsProcessed(totalCount);
            syncJob.setCompletedAt(Instant.now());
            syncJobRepository.save(syncJob);
            log.info("Full vector re-indexing completed successfully. Total entities indexed: {}", totalCount);
        } catch (Exception e) {
            log.error("Failed full vector re-indexing: {}", e.getMessage(), e);
            syncJob.setStatus("FAILED");
            syncJob.setErrorMessage(e.getMessage());
            syncJob.setCompletedAt(Instant.now());
            syncJobRepository.save(syncJob);
        }
    }
}
