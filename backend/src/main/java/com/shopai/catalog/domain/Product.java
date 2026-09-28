package com.shopai.catalog.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shopify_product_id", nullable = false, unique = true)
    private Long shopifyProductId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String handle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String vendor;

    @Column(name = "product_type", length = 255)
    private String productType;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags = new String[0];

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "total_inventory", nullable = false)
    private Integer totalInventory = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "shopify_created_at")
    private Instant shopifyCreatedAt;

    @Column(name = "shopify_updated_at")
    private Instant shopifyUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("product")
    private List<ProductVariant> variants = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_collections",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "collection_id")
    )
    @JsonIgnoreProperties("products")
    private Set<Collection> collections = new HashSet<>();

    public Product() {}

    public Product(Long shopifyProductId, String title, String handle) {
        this.shopifyProductId = shopifyProductId;
        this.title = title;
        this.handle = handle;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getShopifyProductId() { return shopifyProductId; }
    public void setShopifyProductId(Long shopifyProductId) { this.shopifyProductId = shopifyProductId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags != null ? tags : new String[0]; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalInventory() { return totalInventory; }
    public void setTotalInventory(Integer totalInventory) { this.totalInventory = totalInventory; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getShopifyCreatedAt() { return shopifyCreatedAt; }
    public void setShopifyCreatedAt(Instant shopifyCreatedAt) { this.shopifyCreatedAt = shopifyCreatedAt; }

    public Instant getShopifyUpdatedAt() { return shopifyUpdatedAt; }
    public void setShopifyUpdatedAt(Instant shopifyUpdatedAt) { this.shopifyUpdatedAt = shopifyUpdatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }

    public Set<Collection> getCollections() { return collections; }
    public void setCollections(Set<Collection> collections) { this.collections = collections; }

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }
}
