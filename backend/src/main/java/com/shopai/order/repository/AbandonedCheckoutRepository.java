package com.shopai.order.repository;

import com.shopai.order.domain.AbandonedCheckout;
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
public interface AbandonedCheckoutRepository extends JpaRepository<AbandonedCheckout, UUID> {

    Optional<AbandonedCheckout> findByShopifyCheckoutId(Long shopifyCheckoutId);

    @Query("""
        SELECT a FROM AbandonedCheckout a
        WHERE (CAST(:search AS string) IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(a.customerName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(a.cartToken) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:recoveryStatus AS string) IS NULL OR a.recoveryStatus = :recoveryStatus)
    """)
    Page<AbandonedCheckout> findWithFilters(
            @Param("search") String search,
            @Param("recoveryStatus") String recoveryStatus,
            Pageable pageable
    );

    long countByRecoveryStatus(String recoveryStatus);

    @Query("SELECT COALESCE(SUM(a.totalPrice), 0) FROM AbandonedCheckout a WHERE a.recoveryStatus = 'ABANDONED'")
    BigDecimal sumAbandonedTotal();
}
