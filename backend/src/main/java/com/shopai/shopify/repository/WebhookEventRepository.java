package com.shopai.shopify.repository;

import com.shopai.shopify.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByShopifyWebhookId(String shopifyWebhookId);
    List<WebhookEvent> findTop50ByOrderByCreatedAtDesc();
    List<WebhookEvent> findByStatusOrderByCreatedAtAsc(String status);
}
