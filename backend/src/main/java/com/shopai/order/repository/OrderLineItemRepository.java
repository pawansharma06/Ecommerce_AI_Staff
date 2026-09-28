package com.shopai.order.repository;

import com.shopai.order.domain.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderLineItemRepository extends JpaRepository<OrderLineItem, UUID> {
    Optional<OrderLineItem> findByShopifyLineItemId(Long shopifyLineItemId);
    List<OrderLineItem> findByOrderId(UUID orderId);
}
