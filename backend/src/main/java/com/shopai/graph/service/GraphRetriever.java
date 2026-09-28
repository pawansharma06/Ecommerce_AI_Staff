package com.shopai.graph.service;

import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.graph.domain.CommerceRelationship;
import com.shopai.graph.dto.GraphDtos.*;
import com.shopai.graph.repository.GraphRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GraphRetriever {

    private final GraphRepository graphRepository;
    private final ProductRepository productRepository;

    public GraphRetriever(GraphRepository graphRepository, ProductRepository productRepository) {
        this.graphRepository = graphRepository;
        this.productRepository = productRepository;
    }

    public List<Map<String, Object>> getCustomerPurchaseHistory(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) return List.of();
        String normalizedEmail = customerEmail.toLowerCase().trim();

        List<CommerceRelationship> purchasedEdges = graphRepository.findOutboundEdgesByType(
                "Customer", normalizedEmail, "PURCHASED"
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (CommerceRelationship edge : purchasedEdges) {
            String prodId = edge.getTargetId();
            String prodTitle = resolveProductTitle(prodId);

            Map<String, Object> item = new HashMap<>();
            item.put("productId", prodId);
            item.put("productTitle", prodTitle);
            item.put("quantityPurchased", edge.getWeight());
            item.put("confidence", edge.getConfidence());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> findFrequentlyBoughtProducts(String productId, int limit) {
        if (productId == null || productId.isBlank()) return List.of();

        List<CommerceRelationship> fbwEdges = graphRepository.findOutboundEdgesByType(
                "Product", productId, "FREQUENTLY_BOUGHT_WITH"
        );

        // Sort by weight descending
        fbwEdges.sort((a, b) -> b.getWeight().compareTo(a.getWeight()));

        return fbwEdges.stream()
                .limit(limit > 0 ? limit : 5)
                .map(edge -> {
                    String targetId = edge.getTargetId();
                    Map<String, Object> map = new HashMap<>();
                    map.put("productId", targetId);
                    map.put("productTitle", resolveProductTitle(targetId));
                    map.put("coOccurrenceCount", edge.getWeight());
                    map.put("affinityScore", edge.getConfidence());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> findRelatedProducts(String productId, int limit) {
        if (productId == null || productId.isBlank()) return List.of();

        List<CommerceRelationship> outbound = graphRepository.findOutboundEdges("Product", productId);

        List<CommerceRelationship> related = outbound.stream()
                .filter(e -> "FREQUENTLY_BOUGHT_WITH".equals(e.getRelationshipType())
                        || "ALTERNATIVE_TO".equals(e.getRelationshipType())
                        || "UPSELL_TO".equals(e.getRelationshipType()))
                .sorted((a, b) -> b.getWeight().compareTo(a.getWeight()))
                .limit(limit > 0 ? limit : 5)
                .toList();

        return related.stream()
                .map(edge -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productId", edge.getTargetId());
                    map.put("productTitle", resolveProductTitle(edge.getTargetId()));
                    map.put("relationshipType", edge.getRelationshipType());
                    map.put("weight", edge.getWeight());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public GraphSubgraphDto exploreSubgraph(String entityType, String entityId, int maxDepth, int limit) {
        Set<String> visitedNodes = new HashSet<>();
        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();

        Queue<String[]> queue = new LinkedList<>(); // [type, id, depth]

        if (entityType != null && entityId != null && !entityType.isBlank() && !entityId.isBlank()) {
            queue.add(new String[]{entityType, entityId, "0"});
        } else {
            // Sample top nodes across catalog & customers
            List<Product> sampleProds = productRepository.findAll();
            for (int i = 0; i < Math.min(5, sampleProds.size()); i++) {
                Product p = sampleProds.get(i);
                queue.add(new String[]{"Product", p.getId().toString(), "0"});
            }
        }

        int maxNodes = limit > 0 ? limit : 50;
        int depthLimit = maxDepth > 0 ? maxDepth : 2;

        while (!queue.isEmpty() && nodes.size() < maxNodes) {
            String[] current = queue.poll();
            String cType = current[0];
            String cId = current[1];
            int depth = Integer.parseInt(current[2]);

            String nodeKey = cType + ":" + cId;
            if (visitedNodes.contains(nodeKey)) continue;
            visitedNodes.add(nodeKey);

            String label = resolveNodeLabel(cType, cId);
            nodes.add(new GraphNodeDto(cId, label, cType, Map.of()));

            if (depth < depthLimit) {
                List<CommerceRelationship> adj = graphRepository.findAdjacentEdges(cType, cId);
                for (CommerceRelationship rel : adj) {
                    edges.add(new GraphEdgeDto(
                            rel.getId() != null ? rel.getId().toString() : UUID.randomUUID().toString(),
                            rel.getSourceId(),
                            rel.getSourceType(),
                            rel.getTargetId(),
                            rel.getTargetType(),
                            rel.getRelationshipType(),
                            rel.getWeight(),
                            rel.getConfidence(),
                            rel.getMetadata()
                    ));

                    String nextType = rel.getSourceId().equals(cId) ? rel.getTargetType() : rel.getSourceType();
                    String nextId = rel.getSourceId().equals(cId) ? rel.getTargetId() : rel.getSourceId();
                    String nextKey = nextType + ":" + nextId;

                    if (!visitedNodes.contains(nextKey)) {
                        queue.add(new String[]{nextType, nextId, String.valueOf(depth + 1)});
                    }
                }
            }
        }

        return new GraphSubgraphDto(nodes, edges);
    }

    private String resolveProductTitle(String prodIdOrHandle) {
        try {
            UUID id = UUID.fromString(prodIdOrHandle);
            return productRepository.findById(id).map(Product::getTitle).orElse(prodIdOrHandle);
        } catch (IllegalArgumentException e) {
            try {
                Long shopifyId = Long.parseLong(prodIdOrHandle);
                return productRepository.findByShopifyProductId(shopifyId).map(Product::getTitle).orElse(prodIdOrHandle);
            } catch (NumberFormatException nfe) {
                return productRepository.findByHandle(prodIdOrHandle).map(Product::getTitle).orElse(prodIdOrHandle);
            }
        }
    }

    private String resolveNodeLabel(String type, String id) {
        if ("Product".equalsIgnoreCase(type)) {
            return resolveProductTitle(id);
        } else if ("Customer".equalsIgnoreCase(type)) {
            return id;
        } else if ("Order".equalsIgnoreCase(type)) {
            return "Order #" + id.substring(0, Math.min(8, id.length()));
        } else if ("Vendor".equalsIgnoreCase(type)) {
            return "Vendor: " + id;
        }
        return id;
    }
}
