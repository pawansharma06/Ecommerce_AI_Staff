package com.shopai.catalog.repository;

import com.shopai.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByShopifyProductId(Long shopifyProductId);

    Optional<Product> findByHandle(String handle);

    @Query("""
        SELECT p FROM Product p
        WHERE (CAST(:search AS string) IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(p.vendor) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(p.handle) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:status AS string) IS NULL OR p.status = :status)
          AND (CAST(:vendor AS string) IS NULL OR p.vendor = :vendor)
    """)
    Page<Product> findWithFilters(
            @Param("search") String search,
            @Param("status") String status,
            @Param("vendor") String vendor,
            Pageable pageable
    );

    long countByStatus(String status);
}
