package com.shopai.graph;

import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.CollectionRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.graph.dto.GraphDtos.RebuildGraphResponse;
import com.shopai.graph.repository.GraphRepository;
import com.shopai.graph.service.CommerceGraphService;
import com.shopai.order.domain.Order;
import com.shopai.order.domain.OrderLineItem;
import com.shopai.order.repository.OrderLineItemRepository;
import com.shopai.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CommerceGraphServiceTest {

    private GraphRepository graphRepository;
    private ProductRepository productRepository;
    private CollectionRepository collectionRepository;
    private OrderRepository orderRepository;
    private OrderLineItemRepository orderLineItemRepository;
    private CommerceGraphService graphService;

    @BeforeEach
    void setup() {
        graphRepository = Mockito.mock(GraphRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        collectionRepository = Mockito.mock(CollectionRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        orderLineItemRepository = Mockito.mock(OrderLineItemRepository.class);

        graphService = new CommerceGraphService(
                graphRepository,
                productRepository,
                collectionRepository,
                orderRepository,
                orderLineItemRepository
        );
    }

    @Test
    @DisplayName("rebuildKnowledgeGraph mines PLACED, CONTAINS, PURCHASED, and FREQUENTLY_BOUGHT_WITH edges")
    void testRebuildKnowledgeGraph() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();

        Product p1 = new Product(101L, "Alpine Snowboard", "alpine-snowboard");
        p1.setId(prodId1);
        p1.setVendor("SnowCorp");

        Product p2 = new Product(102L, "Snowboard Boots", "snowboard-boots");
        p2.setId(prodId2);
        p2.setVendor("SnowCorp");

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));
        when(collectionRepository.findAll()).thenReturn(List.of());

        UUID orderId = UUID.randomUUID();
        Order order = new Order(1001L, "#1001", "Order #1001");
        order.setId(orderId);
        order.setCustomerEmail("jane@example.com");
        order.setTotalPrice(BigDecimal.valueOf(450.00));

        when(orderRepository.findAll()).thenReturn(List.of(order));

        OrderLineItem item1 = new OrderLineItem(order, 1L, "Alpine Snowboard", 1, BigDecimal.valueOf(350.00));
        item1.setProductId(prodId1);

        OrderLineItem item2 = new OrderLineItem(order, 2L, "Snowboard Boots", 1, BigDecimal.valueOf(100.00));
        item2.setProductId(prodId2);

        when(orderLineItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1, item2));
        when(graphRepository.getRelationshipTypeCounts()).thenReturn(Map.of(
                "PLACED", 1L,
                "CONTAINS", 2L,
                "PURCHASED", 2L,
                "FREQUENTLY_BOUGHT_WITH", 2L,
                "CREATED_BY", 2L
        ));

        RebuildGraphResponse response = graphService.rebuildKnowledgeGraph();

        assertNotNull(response);
        assertTrue(response.edgesCreated() > 0);
        verify(graphRepository, times(1)).deleteAllEdges();
        verify(graphRepository, times(1)).saveAllEdges(anyList());
    }
}
