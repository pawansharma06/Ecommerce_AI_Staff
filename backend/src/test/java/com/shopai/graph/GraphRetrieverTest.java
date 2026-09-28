package com.shopai.graph;

import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.graph.domain.CommerceRelationship;
import com.shopai.graph.dto.GraphDtos.GraphSubgraphDto;
import com.shopai.graph.repository.GraphRepository;
import com.shopai.graph.service.GraphRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GraphRetrieverTest {

    private GraphRepository graphRepository;
    private ProductRepository productRepository;
    private GraphRetriever retriever;

    @BeforeEach
    void setup() {
        graphRepository = Mockito.mock(GraphRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        retriever = new GraphRetriever(graphRepository, productRepository);
    }

    @Test
    @DisplayName("getCustomerPurchaseHistory retrieves products purchased by email")
    void testGetCustomerPurchaseHistory() {
        UUID prodId = UUID.randomUUID();
        Product product = new Product(101L, "Winter Parka", "winter-parka");
        product.setId(prodId);

        when(productRepository.findById(prodId)).thenReturn(Optional.of(product));

        CommerceRelationship rel = new CommerceRelationship(
                "Customer", "customer@example.com", "PURCHASED", "Product", prodId.toString(),
                BigDecimal.valueOf(2), BigDecimal.ONE
        );

        when(graphRepository.findOutboundEdgesByType("Customer", "customer@example.com", "PURCHASED"))
                .thenReturn(List.of(rel));

        List<Map<String, Object>> result = retriever.getCustomerPurchaseHistory("customer@example.com");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Winter Parka", result.get(0).get("productTitle"));
        assertEquals(BigDecimal.valueOf(2), result.get(0).get("quantityPurchased"));
    }

    @Test
    @DisplayName("findFrequentlyBoughtProducts orders by weight descending")
    void testFindFrequentlyBoughtProducts() {
        CommerceRelationship r1 = new CommerceRelationship(
                "Product", "p-1", "FREQUENTLY_BOUGHT_WITH", "Product", "p-boots",
                BigDecimal.valueOf(5), BigDecimal.valueOf(0.9)
        );
        CommerceRelationship r2 = new CommerceRelationship(
                "Product", "p-1", "FREQUENTLY_BOUGHT_WITH", "Product", "p-helmet",
                BigDecimal.valueOf(10), BigDecimal.valueOf(0.98)
        );

        when(graphRepository.findOutboundEdgesByType("Product", "p-1", "FREQUENTLY_BOUGHT_WITH"))
                .thenReturn(new java.util.ArrayList<>(List.of(r1, r2)));

        List<Map<String, Object>> result = retriever.findFrequentlyBoughtProducts("p-1", 5);

        assertNotNull(result);
        assertEquals(2, result.size());
        // Highest weight first
        assertEquals("p-helmet", result.get(0).get("productId"));
        assertEquals(BigDecimal.valueOf(10), result.get(0).get("coOccurrenceCount"));
    }

    @Test
    @DisplayName("exploreSubgraph traverses adjacent edges")
    void testExploreSubgraph() {
        CommerceRelationship r1 = new CommerceRelationship(
                "Product", "p-1", "FREQUENTLY_BOUGHT_WITH", "Product", "p-2",
                BigDecimal.valueOf(4), BigDecimal.valueOf(0.9)
        );

        when(graphRepository.findAdjacentEdges("Product", "p-1")).thenReturn(List.of(r1));
        when(graphRepository.findAdjacentEdges("Product", "p-2")).thenReturn(List.of());

        GraphSubgraphDto subgraph = retriever.exploreSubgraph("Product", "p-1", 2, 20);

        assertNotNull(subgraph);
        assertFalse(subgraph.nodes().isEmpty());
        assertEquals(1, subgraph.edges().size());
    }
}
