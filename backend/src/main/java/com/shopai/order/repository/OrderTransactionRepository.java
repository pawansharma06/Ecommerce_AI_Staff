package com.shopai.order.repository;

import com.shopai.order.domain.OrderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, UUID> {
    Optional<OrderTransaction> findByShopifyTransactionId(Long shopifyTransactionId);
    List<OrderTransaction> findByOrderId(UUID orderId);
}
