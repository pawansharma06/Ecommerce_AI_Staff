package com.shopai.graph.service;

import com.shopai.catalog.domain.Collection;
import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.CollectionRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.graph.domain.CommerceRelationship;
import com.shopai.graph.dto.GraphDtos.*;
import com.shopai.graph.repository.GraphRepository;
import com.shopai.order.domain.Order;
import com.shopai.order.domain.OrderLineItem;
import com.shopai.order.repository.OrderLineItemRepository;
import com.shopai.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CommerceGraphService {

    private static final Logger log = LoggerFactory.getLogger(CommerceGraphService.class);

    private final GraphRepository graphRepository;
    private final ProductRepository productRepository;
    private final CollectionRepository collectionRepository;
    private final OrderRepository orderRepository;
    private final OrderLineItemRepository orderLineItemRepository;

    public CommerceGraphService(
            GraphRepository graphRepository,
            ProductRepository productRepository,
            CollectionRepository collectionRepository,
            OrderRepository orderRepository,
            OrderLineItemRepository orderLineItemRepository
    ) {
        this.graphRepository = graphRepository;
        this.productRepository = productRepository;
        this.collectionRepository = collectionRepository;
        this.orderRepository = orderRepository;
        this.orderLineItemRepository = orderLineItemRepository;
    }

    @Transactional
    public RebuildGraphResponse rebuildKnowledgeGraph() {
        long startTime = System.currentTimeMillis();
        log.info("Starting Commerce Knowledge Graph rebuild & mining...");

        graphRepository.deleteAllEdges();

        List<CommerceRelationship> edges = new ArrayList<>();

        // 1. Process Products & Collections (BELONGS_TO, CREATED_BY)
        List<Product> products = productRepository.findAll();
        Map<String, Product> productMap = new HashMap<>();
        for (Product p : products) {
            if (p.getId() != null) {
                productMap.put(p.getId().toString(), p);
            }
            if (p.getShopifyProductId() != null) {
                productMap.put(p.getShopifyProductId().toString(), p);
            }
            if (p.getVendor() != null && !p.getVendor().isBlank()) {
                edges.add(new CommerceRelationship(
                        "Product", p.getId().toString(),
                        "CREATED_BY",
                        "Vendor", p.getVendor().trim(),
                        BigDecimal.ONE, BigDecimal.ONE
                ));
            }
        }

        List<Collection> collections = collectionRepository.findAll();
        for (Collection c : collections) {
            // If collection references products or vendor rules
            // In a future phase or custom joins we link collections; for now vendor / tags
        }

        // 2. Process Orders & Line Items (PLACED, CONTAINS, PURCHASED)
        List<Order> orders = orderRepository.findAll();
        Map<String, Map<String, Integer>> coOccurrenceMatrix = new HashMap<>();
        Map<String, Map<String, Integer>> customerProductCounts = new HashMap<>();

        for (Order order : orders) {
            String orderId = order.getId().toString();
            String customerEmail = order.getCustomerEmail();

            if (customerEmail != null && !customerEmail.isBlank()) {
                CommerceRelationship placedEdge = new CommerceRelationship(
                        "Customer", customerEmail.toLowerCase().trim(),
                        "PLACED",
                        "Order", orderId,
                        BigDecimal.ONE, BigDecimal.ONE
                );
                placedEdge.setMetadata(Map.of(
                        "orderNumber", order.getOrderNumber() != null ? order.getOrderNumber() : order.getName(),
                        "totalPrice", order.getTotalPrice() != null ? order.getTotalPrice().toString() : "0.00",
                        "currency", order.getCurrency() != null ? order.getCurrency() : "USD"
                ));
                edges.add(placedEdge);
            }

            List<OrderLineItem> items = orderLineItemRepository.findByOrderId(order.getId());
            Set<String> orderProductIds = new HashSet<>();

            for (OrderLineItem item : items) {
                String prodId = item.getProductId() != null ? item.getProductId().toString() : item.getTitle();
                if (prodId == null || prodId.isBlank()) {
                    prodId = "Item-" + item.getId();
                }

                orderProductIds.add(prodId);

                // Order -> CONTAINS -> Product
                CommerceRelationship containsEdge = new CommerceRelationship(
                        "Order", orderId,
                        "CONTAINS",
                        "Product", prodId,
                        BigDecimal.valueOf(item.getQuantity() > 0 ? item.getQuantity() : 1),
                        BigDecimal.ONE
                );
                containsEdge.setMetadata(Map.of(
                        "itemTitle", item.getTitle() != null ? item.getTitle() : "Item",
                        "price", item.getPrice() != null ? item.getPrice().toString() : "0.00"
                ));
                edges.add(containsEdge);

                // Track Customer -> PURCHASED -> Product
                if (customerEmail != null && !customerEmail.isBlank()) {
                    String custKey = customerEmail.toLowerCase().trim();
                    customerProductCounts.computeIfAbsent(custKey, k -> new HashMap<>())
                            .merge(prodId, item.getQuantity() > 0 ? item.getQuantity() : 1, Integer::sum);
                }
            }

            // Mine FREQUENTLY_BOUGHT_WITH co-occurrences
            List<String> prodList = new ArrayList<>(orderProductIds);
            for (int i = 0; i < prodList.size(); i++) {
                for (int j = i + 1; j < prodList.size(); j++) {
                    String p1 = prodList.get(i);
                    String p2 = prodList.get(j);
                    coOccurrenceMatrix.computeIfAbsent(p1, k -> new HashMap<>()).merge(p2, 1, Integer::sum);
                    coOccurrenceMatrix.computeIfAbsent(p2, k -> new HashMap<>()).merge(p1, 1, Integer::sum);
                }
            }
        }

        // 3. Build Customer -> PURCHASED -> Product aggregated edges
        for (Map.Entry<String, Map<String, Integer>> custEntry : customerProductCounts.entrySet()) {
            String custEmail = custEntry.getKey();
            for (Map.Entry<String, Integer> prodEntry : custEntry.getValue().entrySet()) {
                CommerceRelationship purchasedEdge = new CommerceRelationship(
                        "Customer", custEmail,
                        "PURCHASED",
                        "Product", prodEntry.getKey(),
                        BigDecimal.valueOf(prodEntry.getValue()),
                        BigDecimal.ONE
                );
                edges.add(purchasedEdge);
            }
        }

        // 4. Build Product -> FREQUENTLY_BOUGHT_WITH -> Product edges
        for (Map.Entry<String, Map<String, Integer>> entry : coOccurrenceMatrix.entrySet()) {
            String p1 = entry.getKey();
            for (Map.Entry<String, Integer> neighbor : entry.getValue().entrySet()) {
                String p2 = neighbor.getKey();
                int count = neighbor.getValue();
                CommerceRelationship fbw = new CommerceRelationship(
                        "Product", p1,
                        "FREQUENTLY_BOUGHT_WITH",
                        "Product", p2,
                        BigDecimal.valueOf(count),
                        BigDecimal.valueOf(Math.min(1.0, 0.5 + (count * 0.1))).setScale(4, RoundingMode.HALF_UP)
                );
                edges.add(fbw);
            }
        }

        // 5. Synthesize Product -> ALTERNATIVE_TO / COMPLEMENTARY for rich catalog graph
        for (int i = 0; i < products.size(); i++) {
            for (int j = i + 1; j < products.size(); j++) {
                Product pA = products.get(i);
                Product pB = products.get(j);
                if (pA.getId() != null && pB.getId() != null) {
                    // If same vendor or product type, mark as ALTERNATIVE_TO
                    if (pA.getProductType() != null && pA.getProductType().equalsIgnoreCase(pB.getProductType())
                            && !pA.getProductType().isBlank()) {
                        edges.add(new CommerceRelationship(
                                "Product", pA.getId().toString(),
                                "ALTERNATIVE_TO",
                                "Product", pB.getId().toString(),
                                BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.85)
                        ));
                    }
                }
            }
        }

        graphRepository.saveAllEdges(edges);
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Long> counts = graphRepository.getRelationshipTypeCounts();

        log.info("Commerce Knowledge Graph rebuilt in {}ms: {} total edges created across {} relationship types.",
                duration, edges.size(), counts.size());

        return new RebuildGraphResponse(
                edges.size(),
                duration,
                counts,
                "Commerce Knowledge Graph successfully mined and rebuilt."
        );
    }

    public GraphStatsDto getGraphStats() {
        long totalEdges = graphRepository.countTotalEdges();
        Map<String, Long> counts = graphRepository.getRelationshipTypeCounts();
        int totalNodes = (int) (productRepository.count() + orderRepository.count());
        return new GraphStatsDto(totalEdges, totalNodes, counts);
    }
}
