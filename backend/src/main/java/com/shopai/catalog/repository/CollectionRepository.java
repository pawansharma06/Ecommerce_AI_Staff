package com.shopai.catalog.repository;

import com.shopai.catalog.domain.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {
    Optional<Collection> findByShopifyCollectionId(Long shopifyCollectionId);
    Optional<Collection> findByHandle(String handle);
}
