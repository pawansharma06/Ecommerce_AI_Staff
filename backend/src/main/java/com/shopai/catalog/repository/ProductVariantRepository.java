package com.shopai.catalog.repository;

import com.shopai.catalog.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    Optional<ProductVariant> findByShopifyVariantId(Long shopifyVariantId);
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductId(UUID productId);
}
