package com.shopai.shopify.repository;

import com.shopai.shopify.domain.ShopifyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopifyConfigRepository extends JpaRepository<ShopifyConfig, UUID> {
    Optional<ShopifyConfig> findByShopDomain(String shopDomain);
    Optional<ShopifyConfig> findFirstByOrderByCreatedAtDesc();
}
