package com.shopai.order.repository;

import com.shopai.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByShopifyOrderId(Long shopifyOrderId);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("""
        SELECT o FROM Order o
        WHERE (CAST(:search AS string) IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(o.customerFirstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(o.customerLastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:financialStatus AS string) IS NULL OR o.financialStatus = :financialStatus)
          AND (CAST(:fulfillmentStatus AS string) IS NULL OR o.fulfillmentStatus = :fulfillmentStatus)
    """)
    Page<Order> findWithFilters(
            @Param("search") String search,
            @Param("financialStatus") String financialStatus,
            @Param("fulfillmentStatus") String fulfillmentStatus,
            Pageable pageable
    );

    long countByFulfillmentStatus(String fulfillmentStatus);

    long countByFinancialStatus(String financialStatus);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.financialStatus IN ('PAID', 'PARTIALLY_REFUNDED')")
    BigDecimal sumTotalSales();
}
