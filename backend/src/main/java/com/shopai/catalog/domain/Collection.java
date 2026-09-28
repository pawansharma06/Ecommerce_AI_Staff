package com.shopai.catalog.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shopify_collection_id", nullable = false, unique = true)
    private Long shopifyCollectionId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String handle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "collection_type", nullable = false, length = 50)
    private String collectionType = "CUSTOM";

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @ManyToMany(mappedBy = "collections", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Product> products = new HashSet<>();

    public Collection() {}

    public Collection(Long shopifyCollectionId, String title, String handle) {
        this.shopifyCollectionId = shopifyCollectionId;
        this.title = title;
        this.handle = handle;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getShopifyCollectionId() { return shopifyCollectionId; }
    public void setShopifyCollectionId(Long shopifyCollectionId) { this.shopifyCollectionId = shopifyCollectionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCollectionType() { return collectionType; }
    public void setCollectionType(String collectionType) { this.collectionType = collectionType; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public Set<Product> getProducts() { return products; }
    public void setProducts(Set<Product> products) { this.products = products; }
}
