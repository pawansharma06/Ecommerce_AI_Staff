package com.shopai.order.repository;

import com.shopai.order.domain.Fulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FulfillmentRepository extends JpaRepository<Fulfillment, UUID> {
    Optional<Fulfillment> findByShopifyFulfillmentId(Long shopifyFulfillmentId);
    List<Fulfillment> findByOrderId(UUID orderId);
}
