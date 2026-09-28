import os

def write_file(rel_path, content):
    full_path = os.path.join(r'c:\apps\ShopAI\backend\src\main\java\com\shopai', rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print('Created:', rel_path)

# ==============================================================================
# 1. CATALOG ENTITIES, REPOSITORIES, DTOS, SERVICE, CONTROLLER
# ==============================================================================

write_file('catalog/domain/Product.java', '''package com.shopai.catalog.domain;

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

    @Column(name = "tags")
    private String tags;

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

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

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
''')

write_file('catalog/domain/ProductVariant.java', '''package com.shopai.catalog.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @Column(name = "shopify_variant_id", nullable = false, unique = true)
    private Long shopifyVariantId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String sku;

    @Column(length = 100)
    private String barcode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "compare_at_price", precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "inventory_quantity", nullable = false)
    private Integer inventoryQuantity = 0;

    @Column(nullable = false)
    private Integer position = 1;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "requires_shipping", nullable = false)
    private Boolean requiresShipping = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProductVariant() {}

    public ProductVariant(Product product, Long shopifyVariantId, String title, BigDecimal price) {
        this.product = product;
        this.shopifyVariantId = shopifyVariantId;
        this.title = title;
        this.price = price;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Long getShopifyVariantId() { return shopifyVariantId; }
    public void setShopifyVariantId(Long shopifyVariantId) { this.shopifyVariantId = shopifyVariantId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }

    public Integer getInventoryQuantity() { return inventoryQuantity; }
    public void setInventoryQuantity(Integer inventoryQuantity) { this.inventoryQuantity = inventoryQuantity; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getRequiresShipping() { return requiresShipping; }
    public void setRequiresShipping(Boolean requiresShipping) { this.requiresShipping = requiresShipping; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
''')

write_file('catalog/domain/Collection.java', '''package com.shopai.catalog.domain;

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
''')

write_file('catalog/repository/ProductRepository.java', '''package com.shopai.catalog.repository;

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
        WHERE (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.vendor) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.handle) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR p.status = :status)
          AND (:vendor IS NULL OR p.vendor = :vendor)
    """)
    Page<Product> findWithFilters(
            @Param("search") String search,
            @Param("status") String status,
            @Param("vendor") String vendor,
            Pageable pageable
    );

    long countByStatus(String status);
}
''')

write_file('catalog/repository/ProductVariantRepository.java', '''package com.shopai.catalog.repository;

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
''')

write_file('catalog/repository/CollectionRepository.java', '''package com.shopai.catalog.repository;

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
''')

write_file('catalog/dto/ProductVariantResponse.java', '''package com.shopai.catalog.dto;

import com.shopai.catalog.domain.ProductVariant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        Long shopifyVariantId,
        String title,
        String sku,
        String barcode,
        BigDecimal price,
        BigDecimal compareAtPrice,
        Integer inventoryQuantity,
        Integer position,
        String imageUrl,
        Boolean requiresShipping,
        Instant updatedAt
) {
    public static ProductVariantResponse from(ProductVariant v) {
        return new ProductVariantResponse(
                v.getId(),
                v.getShopifyVariantId(),
                v.getTitle(),
                v.getSku(),
                v.getBarcode(),
                v.getPrice(),
                v.getCompareAtPrice(),
                v.getInventoryQuantity(),
                v.getPosition(),
                v.getImageUrl(),
                v.getRequiresShipping(),
                v.getUpdatedAt()
        );
    }
}
''')

write_file('catalog/dto/CollectionResponse.java', '''package com.shopai.catalog.dto;

import com.shopai.catalog.domain.Collection;
import java.time.Instant;
import java.util.UUID;

public record CollectionResponse(
        UUID id,
        Long shopifyCollectionId,
        String title,
        String handle,
        String description,
        String collectionType,
        Instant syncedAt
) {
    public static CollectionResponse from(Collection c) {
        return new CollectionResponse(
                c.getId(),
                c.getShopifyCollectionId(),
                c.getTitle(),
                c.getHandle(),
                c.getDescription(),
                c.getCollectionType(),
                c.getSyncedAt()
        );
    }
}
''')

write_file('catalog/dto/ProductResponse.java', '''package com.shopai.catalog.dto;

import com.shopai.catalog.domain.Product;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProductResponse(
        UUID id,
        Long shopifyProductId,
        String title,
        String handle,
        String description,
        String vendor,
        String productType,
        String tags,
        String status,
        Integer totalInventory,
        Instant publishedAt,
        Instant shopifyCreatedAt,
        Instant shopifyUpdatedAt,
        Instant syncedAt,
        List<ProductVariantResponse> variants,
        List<CollectionResponse> collections
) {
    public static ProductResponse from(Product p) {
        List<ProductVariantResponse> variantResponses = p.getVariants() != null
                ? p.getVariants().stream().map(ProductVariantResponse::from).collect(Collectors.toList())
                : List.of();

        List<CollectionResponse> collectionResponses = p.getCollections() != null
                ? p.getCollections().stream().map(CollectionResponse::from).collect(Collectors.toList())
                : List.of();

        return new ProductResponse(
                p.getId(),
                p.getShopifyProductId(),
                p.getTitle(),
                p.getHandle(),
                p.getDescription(),
                p.getVendor(),
                p.getProductType(),
                p.getTags(),
                p.getStatus(),
                p.getTotalInventory(),
                p.getPublishedAt(),
                p.getShopifyCreatedAt(),
                p.getShopifyUpdatedAt(),
                p.getSyncedAt(),
                variantResponses,
                collectionResponses
        );
    }
}
''')

write_file('catalog/dto/CatalogStatsResponse.java', '''package com.shopai.catalog.dto;

public record CatalogStatsResponse(
        long totalProducts,
        long activeProducts,
        long draftProducts,
        long archivedProducts,
        long totalVariants,
        long lowStockVariants
) {}
''')

write_file('catalog/service/ProductService.java', '''package com.shopai.catalog.service;

import com.shopai.catalog.domain.Product;
import com.shopai.catalog.dto.CatalogStatsResponse;
import com.shopai.catalog.dto.CollectionResponse;
import com.shopai.catalog.dto.ProductResponse;
import com.shopai.catalog.repository.CollectionRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.catalog.repository.ProductVariantRepository;
import com.shopai.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CollectionRepository collectionRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CollectionRepository collectionRepository
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.collectionRepository = collectionRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(int page, int size, String search, String status, String vendor) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.trim().toUpperCase() : null;
        String cleanVendor = (vendor != null && !vendor.isBlank()) ? vendor.trim() : null;

        return productRepository.findWithFilters(cleanSearch, cleanStatus, cleanVendor, pageable)
                .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getCollections() {
        return collectionRepository.findAll().stream()
                .map(CollectionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CatalogStatsResponse getCatalogStats() {
        long total = productRepository.count();
        long active = productRepository.countByStatus("ACTIVE");
        long draft = productRepository.countByStatus("DRAFT");
        long archived = productRepository.countByStatus("ARCHIVED");
        long totalVariants = productVariantRepository.count();
        long lowStock = productVariantRepository.findAll().stream()
                .filter(v -> v.getInventoryQuantity() != null && v.getInventoryQuantity() <= 5)
                .count();

        return new CatalogStatsResponse(total, active, draft, archived, totalVariants, lowStock);
    }
}
''')

write_file('catalog/controller/ProductController.java', '''package com.shopai.catalog.controller;

import com.shopai.catalog.dto.CatalogStatsResponse;
import com.shopai.catalog.dto.CollectionResponse;
import com.shopai.catalog.dto.ProductResponse;
import com.shopai.catalog.service.ProductService;
import com.shopai.common.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String vendor
    ) {
        Page<ProductResponse> products = productService.getProducts(page, size, search, status, vendor);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @GetMapping("/collections")
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getCollections() {
        List<CollectionResponse> collections = productService.getCollections();
        return ResponseEntity.ok(ApiResponse.ok(collections));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<CatalogStatsResponse>> getCatalogStats() {
        CatalogStatsResponse stats = productService.getCatalogStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
''')

# ==============================================================================
# 2. ORDER, FULFILLMENT, TRANSACTION & ABANDONED CHECKOUT ENTITIES & REPOSITORIES
# ==============================================================================

write_file('order/domain/Order.java', '''package com.shopai.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shopify_order_id", nullable = false, unique = true)
    private Long shopifyOrderId;

    @Column(name = "order_number", nullable = false, length = 100)
    private String orderNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "financial_status", nullable = false, length = 50)
    private String financialStatus = "PENDING";

    @Column(name = "fulfillment_status", nullable = false, length = 50)
    private String fulfillmentStatus = "UNFULFILLED";

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "subtotal_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalPrice = BigDecimal.ZERO;

    @Column(name = "total_discounts", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDiscounts = BigDecimal.ZERO;

    @Column(name = "total_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "total_shipping", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalShipping = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_first_name", length = 100)
    private String customerFirstName;

    @Column(name = "customer_last_name", length = 100)
    private String customerLastName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "shipping_address", columnDefinition = "jsonb")
    private String shippingAddress;

    @Column(name = "billing_address", columnDefinition = "jsonb")
    private String billingAddress;

    @Column(name = "tags")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "shopify_created_at")
    private Instant shopifyCreatedAt;

    @Column(name = "shopify_updated_at")
    private Instant shopifyUpdatedAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("order")
    private List<OrderLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("order")
    private List<Fulfillment> fulfillments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("order")
    private List<OrderTransaction> transactions = new ArrayList<>();

    public Order() {}

    public Order(Long shopifyOrderId, String orderNumber, String name) {
        this.shopifyOrderId = shopifyOrderId;
        this.orderNumber = orderNumber;
        this.name = name;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getShopifyOrderId() { return shopifyOrderId; }
    public void setShopifyOrderId(Long shopifyOrderId) { this.shopifyOrderId = shopifyOrderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFinancialStatus() { return financialStatus; }
    public void setFinancialStatus(String financialStatus) { this.financialStatus = financialStatus; }

    public String getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(String fulfillmentStatus) { this.fulfillmentStatus = fulfillmentStatus; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getSubtotalPrice() { return subtotalPrice; }
    public void setSubtotalPrice(BigDecimal subtotalPrice) { this.subtotalPrice = subtotalPrice; }

    public BigDecimal getTotalDiscounts() { return totalDiscounts; }
    public void setTotalDiscounts(BigDecimal totalDiscounts) { this.totalDiscounts = totalDiscounts; }

    public BigDecimal getTotalTax() { return totalTax; }
    public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }

    public BigDecimal getTotalShipping() { return totalShipping; }
    public void setTotalShipping(BigDecimal totalShipping) { this.totalShipping = totalShipping; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerFirstName() { return customerFirstName; }
    public void setCustomerFirstName(String customerFirstName) { this.customerFirstName = customerFirstName; }

    public String getCustomerLastName() { return customerLastName; }
    public void setCustomerLastName(String customerLastName) { this.customerLastName = customerLastName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getShopifyCreatedAt() { return shopifyCreatedAt; }
    public void setShopifyCreatedAt(Instant shopifyCreatedAt) { this.shopifyCreatedAt = shopifyCreatedAt; }

    public Instant getShopifyUpdatedAt() { return shopifyUpdatedAt; }
    public void setShopifyUpdatedAt(Instant shopifyUpdatedAt) { this.shopifyUpdatedAt = shopifyUpdatedAt; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<OrderLineItem> lineItems) { this.lineItems = lineItems; }

    public List<Fulfillment> getFulfillments() { return fulfillments; }
    public void setFulfillments(List<Fulfillment> fulfillments) { this.fulfillments = fulfillments; }

    public List<OrderTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<OrderTransaction> transactions) { this.transactions = transactions; }

    public void addLineItem(OrderLineItem item) {
        lineItems.add(item);
        item.setOrder(this);
    }

    public void addFulfillment(Fulfillment fulfillment) {
        fulfillments.add(fulfillment);
        fulfillment.setOrder(this);
    }

    public void addTransaction(OrderTransaction transaction) {
        transactions.add(transaction);
        transaction.setOrder(this);
    }
}
''')

write_file('order/domain/OrderLineItem.java', '''package com.shopai.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_line_items")
public class OrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "shopify_line_item_id", nullable = false)
    private Long shopifyLineItemId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "variant_title", length = 255)
    private String variantTitle;

    @Column(length = 100)
    private String sku;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "fulfillable_quantity", nullable = false)
    private Integer fulfillableQuantity = 0;

    @Column(name = "fulfilled_quantity", nullable = false)
    private Integer fulfilledQuantity = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "total_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "requires_shipping", nullable = false)
    private Boolean requiresShipping = true;

    @Column(nullable = false)
    private Boolean taxable = true;

    public OrderLineItem() {}

    public OrderLineItem(Order order, Long shopifyLineItemId, String title, Integer quantity, BigDecimal price) {
        this.order = order;
        this.shopifyLineItemId = shopifyLineItemId;
        this.title = title;
        this.quantity = quantity;
        this.price = price;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Long getShopifyLineItemId() { return shopifyLineItemId; }
    public void setShopifyLineItemId(Long shopifyLineItemId) { this.shopifyLineItemId = shopifyLineItemId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public UUID getVariantId() { return variantId; }
    public void setVariantId(UUID variantId) { this.variantId = variantId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVariantTitle() { return variantTitle; }
    public void setVariantTitle(String variantTitle) { this.variantTitle = variantTitle; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getFulfillableQuantity() { return fulfillableQuantity; }
    public void setFulfillableQuantity(Integer fulfillableQuantity) { this.fulfillableQuantity = fulfillableQuantity; }

    public Integer getFulfilledQuantity() { return fulfilledQuantity; }
    public void setFulfilledQuantity(Integer fulfilledQuantity) { this.fulfilledQuantity = fulfilledQuantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }

    public Boolean getRequiresShipping() { return requiresShipping; }
    public void setRequiresShipping(Boolean requiresShipping) { this.requiresShipping = requiresShipping; }

    public Boolean getTaxable() { return taxable; }
    public void setTaxable(Boolean taxable) { this.taxable = taxable; }
}
''')

write_file('order/domain/Fulfillment.java', '''package com.shopai.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "fulfillments")
public class Fulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "shopify_fulfillment_id", nullable = false, unique = true)
    private Long shopifyFulfillmentId;

    @Column(nullable = false, length = 50)
    private String status = "SUCCESS";

    @Column(name = "tracking_company", length = 100)
    private String trackingCompany;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "tracking_url", columnDefinition = "TEXT")
    private String trackingUrl;

    @Column(length = 100)
    private String service;

    @Column(name = "shipment_status", length = 50)
    private String shipmentStatus;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "shopify_created_at")
    private Instant shopifyCreatedAt;

    @Column(name = "shopify_updated_at")
    private Instant shopifyUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "fulfillment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("fulfillment")
    private List<FulfillmentLineItem> fulfillmentLineItems = new ArrayList<>();

    public Fulfillment() {}

    public Fulfillment(Order order, Long shopifyFulfillmentId, String status) {
        this.order = order;
        this.shopifyFulfillmentId = shopifyFulfillmentId;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Long getShopifyFulfillmentId() { return shopifyFulfillmentId; }
    public void setShopifyFulfillmentId(Long shopifyFulfillmentId) { this.shopifyFulfillmentId = shopifyFulfillmentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTrackingCompany() { return trackingCompany; }
    public void setTrackingCompany(String trackingCompany) { this.trackingCompany = trackingCompany; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String shipmentStatus) { this.shipmentStatus = shipmentStatus; }

    public Instant getEstimatedDeliveryAt() { return estimatedDeliveryAt; }
    public void setEstimatedDeliveryAt(Instant estimatedDeliveryAt) { this.estimatedDeliveryAt = estimatedDeliveryAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public Instant getShopifyCreatedAt() { return shopifyCreatedAt; }
    public void setShopifyCreatedAt(Instant shopifyCreatedAt) { this.shopifyCreatedAt = shopifyCreatedAt; }

    public Instant getShopifyUpdatedAt() { return shopifyUpdatedAt; }
    public void setShopifyUpdatedAt(Instant shopifyUpdatedAt) { this.shopifyUpdatedAt = shopifyUpdatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<FulfillmentLineItem> getFulfillmentLineItems() { return fulfillmentLineItems; }
    public void setFulfillmentLineItems(List<FulfillmentLineItem> fulfillmentLineItems) { this.fulfillmentLineItems = fulfillmentLineItems; }

    public void addFulfillmentLineItem(FulfillmentLineItem item) {
        fulfillmentLineItems.add(item);
        item.setFulfillment(this);
    }
}
''')

write_file('order/domain/FulfillmentLineItem.java', '''package com.shopai.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "fulfillment_line_items")
public class FulfillmentLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfillment_id", nullable = false)
    @JsonIgnore
    private Fulfillment fulfillment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_line_item_id", nullable = false)
    private OrderLineItem orderLineItem;

    @Column(nullable = false)
    private Integer quantity = 1;

    public FulfillmentLineItem() {}

    public FulfillmentLineItem(Fulfillment fulfillment, OrderLineItem orderLineItem, Integer quantity) {
        this.fulfillment = fulfillment;
        this.orderLineItem = orderLineItem;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Fulfillment getFulfillment() { return fulfillment; }
    public void setFulfillment(Fulfillment fulfillment) { this.fulfillment = fulfillment; }

    public OrderLineItem getOrderLineItem() { return orderLineItem; }
    public void setOrderLineItem(OrderLineItem orderLineItem) { this.orderLineItem = orderLineItem; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
''')

write_file('order/domain/OrderTransaction.java', '''package com.shopai.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_transactions")
public class OrderTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "shopify_transaction_id", nullable = false, unique = true)
    private Long shopifyTransactionId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(length = 100)
    private String gateway;

    @Column(nullable = false, length = 50)
    private String kind; // AUTHORIZATION, CAPTURE, SALE, REFUND, VOID

    @Column(nullable = false, length = 50)
    private String status = "SUCCESS"; // SUCCESS, PENDING, FAILURE, ERROR

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "payment_method_name", length = 100)
    private String paymentMethodName;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OrderTransaction() {}

    public OrderTransaction(Order order, Long shopifyTransactionId, String kind, BigDecimal amount, String currency) {
        this.order = order;
        this.shopifyTransactionId = shopifyTransactionId;
        this.kind = kind;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Long getShopifyTransactionId() { return shopifyTransactionId; }
    public void setShopifyTransactionId(Long shopifyTransactionId) { this.shopifyTransactionId = shopifyTransactionId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
''')

write_file('order/domain/AbandonedCheckout.java', '''package com.shopai.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "abandoned_checkouts")
public class AbandonedCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shopify_checkout_id", nullable = false, unique = true)
    private Long shopifyCheckoutId;

    @Column(name = "cart_token", length = 255)
    private String cartToken;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "subtotal_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalPrice = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "abandoned_checkout_url", columnDefinition = "TEXT")
    private String abandonedCheckoutUrl;

    @Column(name = "recovery_status", nullable = false, length = 50)
    private String recoveryStatus = "ABANDONED"; // ABANDONED, RECOVERED

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "line_items", columnDefinition = "jsonb")
    private String lineItems;

    @Column(name = "shopify_created_at")
    private Instant shopifyCreatedAt;

    @Column(name = "shopify_updated_at")
    private Instant shopifyUpdatedAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AbandonedCheckout() {}

    public AbandonedCheckout(Long shopifyCheckoutId, String email, BigDecimal totalPrice) {
        this.shopifyCheckoutId = shopifyCheckoutId;
        this.email = email;
        this.totalPrice = totalPrice;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getShopifyCheckoutId() { return shopifyCheckoutId; }
    public void setShopifyCheckoutId(Long shopifyCheckoutId) { this.shopifyCheckoutId = shopifyCheckoutId; }

    public String getCartToken() { return cartToken; }
    public void setCartToken(String cartToken) { this.cartToken = cartToken; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public BigDecimal getSubtotalPrice() { return subtotalPrice; }
    public void setSubtotalPrice(BigDecimal subtotalPrice) { this.subtotalPrice = subtotalPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getAbandonedCheckoutUrl() { return abandonedCheckoutUrl; }
    public void setAbandonedCheckoutUrl(String abandonedCheckoutUrl) { this.abandonedCheckoutUrl = abandonedCheckoutUrl; }

    public String getRecoveryStatus() { return recoveryStatus; }
    public void setRecoveryStatus(String recoveryStatus) { this.recoveryStatus = recoveryStatus; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getLineItems() { return lineItems; }
    public void setLineItems(String lineItems) { this.lineItems = lineItems; }

    public Instant getShopifyCreatedAt() { return shopifyCreatedAt; }
    public void setShopifyCreatedAt(Instant shopifyCreatedAt) { this.shopifyCreatedAt = shopifyCreatedAt; }

    public Instant getShopifyUpdatedAt() { return shopifyUpdatedAt; }
    public void setShopifyUpdatedAt(Instant shopifyUpdatedAt) { this.shopifyUpdatedAt = shopifyUpdatedAt; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
''')

write_file('order/repository/OrderRepository.java', '''package com.shopai.order.repository;

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
        WHERE (:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.customerFirstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.customerLastName) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:financialStatus IS NULL OR o.financialStatus = :financialStatus)
          AND (:fulfillmentStatus IS NULL OR o.fulfillmentStatus = :fulfillmentStatus)
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
''')

write_file('order/repository/OrderLineItemRepository.java', '''package com.shopai.order.repository;

import com.shopai.order.domain.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderLineItemRepository extends JpaRepository<OrderLineItem, UUID> {
    Optional<OrderLineItem> findByShopifyLineItemId(Long shopifyLineItemId);
    List<OrderLineItem> findByOrderId(UUID orderId);
}
''')

write_file('order/repository/FulfillmentRepository.java', '''package com.shopai.order.repository;

import com.shopai.order.domain.Fulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FulfillmentRepository extends JpaRepository<Fulfillment, UUID> {
    Optional<Fulfillment> findByShopifyFulfillmentId(Long shopifyFulfillmentId);
    List<Fulfillment> findByOrderId(UUID orderId);
}
''')

write_file('order/repository/OrderTransactionRepository.java', '''package com.shopai.order.repository;

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
''')

write_file('order/repository/AbandonedCheckoutRepository.java', '''package com.shopai.order.repository;

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
        WHERE (:search IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.cartToken) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:recoveryStatus IS NULL OR a.recoveryStatus = :recoveryStatus)
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
''')

write_file('order/dto/OrderLineItemResponse.java', '''package com.shopai.order.dto;

import com.shopai.order.domain.OrderLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineItemResponse(
        UUID id,
        Long shopifyLineItemId,
        UUID productId,
        UUID variantId,
        String title,
        String variantTitle,
        String sku,
        Integer quantity,
        Integer fulfillableQuantity,
        Integer fulfilledQuantity,
        BigDecimal price,
        BigDecimal totalDiscount
) {
    public static OrderLineItemResponse from(OrderLineItem i) {
        return new OrderLineItemResponse(
                i.getId(),
                i.getShopifyLineItemId(),
                i.getProductId(),
                i.getVariantId(),
                i.getTitle(),
                i.getVariantTitle(),
                i.getSku(),
                i.getQuantity(),
                i.getFulfillableQuantity(),
                i.getFulfilledQuantity(),
                i.getPrice(),
                i.getTotalDiscount()
        );
    }
}
''')

write_file('order/dto/FulfillmentLineItemResponse.java', '''package com.shopai.order.dto;

import com.shopai.order.domain.FulfillmentLineItem;
import java.util.UUID;

public record FulfillmentLineItemResponse(
        UUID id,
        UUID orderLineItemId,
        String itemTitle,
        String itemSku,
        Integer quantity
) {
    public static FulfillmentLineItemResponse from(FulfillmentLineItem fli) {
        String title = fli.getOrderLineItem() != null ? fli.getOrderLineItem().getTitle() : "";
        String sku = fli.getOrderLineItem() != null ? fli.getOrderLineItem().getSku() : "";
        UUID lineItemId = fli.getOrderLineItem() != null ? fli.getOrderLineItem().getId() : null;

        return new FulfillmentLineItemResponse(
                fli.getId(),
                lineItemId,
                title,
                sku,
                fli.getQuantity()
        );
    }
}
''')

write_file('order/dto/FulfillmentResponse.java', '''package com.shopai.order.dto;

import com.shopai.order.domain.Fulfillment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record FulfillmentResponse(
        UUID id,
        Long shopifyFulfillmentId,
        String status,
        String trackingCompany,
        String trackingNumber,
        String trackingUrl,
        String service,
        String shipmentStatus,
        Instant estimatedDeliveryAt,
        Instant deliveredAt,
        Instant shopifyCreatedAt,
        List<FulfillmentLineItemResponse> items
) {
    public static FulfillmentResponse from(Fulfillment f) {
        List<FulfillmentLineItemResponse> items = f.getFulfillmentLineItems() != null
                ? f.getFulfillmentLineItems().stream().map(FulfillmentLineItemResponse::from).collect(Collectors.toList())
                : List.of();

        return new FulfillmentResponse(
                f.getId(),
                f.getShopifyFulfillmentId(),
                f.getStatus(),
                f.getTrackingCompany(),
                f.getTrackingNumber(),
                f.getTrackingUrl(),
                f.getService(),
                f.getShipmentStatus(),
                f.getEstimatedDeliveryAt(),
                f.getDeliveredAt(),
                f.getShopifyCreatedAt(),
                items
        );
    }
}
''')

write_file('order/dto/OrderTransactionResponse.java', '''package com.shopai.order.dto;

import com.shopai.order.domain.OrderTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderTransactionResponse(
        UUID id,
        Long shopifyTransactionId,
        Long parentId,
        String gateway,
        String kind,
        String status,
        BigDecimal amount,
        String currency,
        String paymentMethodName,
        String errorCode,
        String errorMessage,
        Instant processedAt
) {
    public static OrderTransactionResponse from(OrderTransaction t) {
        return new OrderTransactionResponse(
                t.getId(),
                t.getShopifyTransactionId(),
                t.getParentId(),
                t.getGateway(),
                t.getKind(),
                t.getStatus(),
                t.getAmount(),
                t.getCurrency(),
                t.getPaymentMethodName(),
                t.getErrorCode(),
                t.getErrorMessage(),
                t.getProcessedAt()
        );
    }
}
''')

write_file('order/dto/OrderResponse.java', '''package com.shopai.order.dto;

import com.shopai.order.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record OrderResponse(
        UUID id,
        Long shopifyOrderId,
        String orderNumber,
        String name,
        String email,
        String phone,
        String financialStatus,
        String fulfillmentStatus,
        String currency,
        BigDecimal subtotalPrice,
        BigDecimal totalDiscounts,
        BigDecimal totalTax,
        BigDecimal totalShipping,
        BigDecimal totalPrice,
        Instant cancelledAt,
        String cancelReason,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String shippingAddress,
        String billingAddress,
        String tags,
        String note,
        Instant shopifyCreatedAt,
        Instant shopifyUpdatedAt,
        Instant syncedAt,
        List<OrderLineItemResponse> lineItems,
        List<FulfillmentResponse> fulfillments,
        List<OrderTransactionResponse> transactions
) {
    public static OrderResponse from(Order o) {
        List<OrderLineItemResponse> items = o.getLineItems() != null
                ? o.getLineItems().stream().map(OrderLineItemResponse::from).collect(Collectors.toList())
                : List.of();

        List<FulfillmentResponse> fulfillments = o.getFulfillments() != null
                ? o.getFulfillments().stream().map(FulfillmentResponse::from).collect(Collectors.toList())
                : List.of();

        List<OrderTransactionResponse> transactions = o.getTransactions() != null
                ? o.getTransactions().stream().map(OrderTransactionResponse::from).collect(Collectors.toList())
                : List.of();

        return new OrderResponse(
                o.getId(),
                o.getShopifyOrderId(),
                o.getOrderNumber(),
                o.getName(),
                o.getEmail(),
                o.getPhone(),
                o.getFinancialStatus(),
                o.getFulfillmentStatus(),
                o.getCurrency(),
                o.getSubtotalPrice(),
                o.getTotalDiscounts(),
                o.getTotalTax(),
                o.getTotalShipping(),
                o.getTotalPrice(),
                o.getCancelledAt(),
                o.getCancelReason(),
                o.getCustomerFirstName(),
                o.getCustomerLastName(),
                o.getCustomerEmail(),
                o.getShippingAddress(),
                o.getBillingAddress(),
                o.getTags(),
                o.getNote(),
                o.getShopifyCreatedAt(),
                o.getShopifyUpdatedAt(),
                o.getSyncedAt(),
                items,
                fulfillments,
                transactions
        );
    }
}
''')

write_file('order/dto/AbandonedCheckoutResponse.java', '''package com.shopai.order.dto;

import com.shopai.order.domain.AbandonedCheckout;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AbandonedCheckoutResponse(
        UUID id,
        Long shopifyCheckoutId,
        String cartToken,
        String email,
        String phone,
        String customerName,
        BigDecimal subtotalPrice,
        BigDecimal totalPrice,
        String currency,
        String abandonedCheckoutUrl,
        String recoveryStatus,
        Instant completedAt,
        String lineItems,
        Instant shopifyCreatedAt,
        Instant syncedAt
) {
    public static AbandonedCheckoutResponse from(AbandonedCheckout a) {
        return new AbandonedCheckoutResponse(
                a.getId(),
                a.getShopifyCheckoutId(),
                a.getCartToken(),
                a.getEmail(),
                a.getPhone(),
                a.getCustomerName(),
                a.getSubtotalPrice(),
                a.getTotalPrice(),
                a.getCurrency(),
                a.getAbandonedCheckoutUrl(),
                a.getRecoveryStatus(),
                a.getCompletedAt(),
                a.getLineItems(),
                a.getShopifyCreatedAt(),
                a.getSyncedAt()
        );
    }
}
''')

write_file('order/dto/OrderStatsResponse.java', '''package com.shopai.order.dto;

import java.math.BigDecimal;

public record OrderStatsResponse(
        long totalOrders,
        long unfulfilledOrders,
        long partiallyFulfilledOrders,
        long fulfilledOrders,
        long paidOrders,
        long pendingOrders,
        BigDecimal totalSales,
        long abandonedCheckoutsCount,
        long recoveredCheckoutsCount,
        BigDecimal totalAbandonedValue
) {}
''')

write_file('order/service/OrderService.java', '''package com.shopai.order.service;

import com.shopai.common.exception.ResourceNotFoundException;
import com.shopai.order.domain.AbandonedCheckout;
import com.shopai.order.domain.Order;
import com.shopai.order.dto.AbandonedCheckoutResponse;
import com.shopai.order.dto.OrderResponse;
import com.shopai.order.dto.OrderStatsResponse;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AbandonedCheckoutRepository abandonedCheckoutRepository;

    public OrderService(
            OrderRepository orderRepository,
            AbandonedCheckoutRepository abandonedCheckoutRepository
    ) {
        this.orderRepository = orderRepository;
        this.abandonedCheckoutRepository = abandonedCheckoutRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(int page, int size, String search, String financialStatus, String fulfillmentStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "shopifyCreatedAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanFinancial = (financialStatus != null && !financialStatus.isBlank() && !"ALL".equalsIgnoreCase(financialStatus)) ? financialStatus.trim().toUpperCase() : null;
        String cleanFulfillment = (fulfillmentStatus != null && !fulfillmentStatus.isBlank() && !"ALL".equalsIgnoreCase(fulfillmentStatus)) ? fulfillmentStatus.trim().toUpperCase() : null;

        return orderRepository.findWithFilters(cleanSearch, cleanFinancial, cleanFulfillment, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<AbandonedCheckoutResponse> getAbandonedCheckouts(int page, int size, String search, String recoveryStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "shopifyCreatedAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (recoveryStatus != null && !recoveryStatus.isBlank() && !"ALL".equalsIgnoreCase(recoveryStatus)) ? recoveryStatus.trim().toUpperCase() : null;

        return abandonedCheckoutRepository.findWithFilters(cleanSearch, cleanStatus, pageable)
                .map(AbandonedCheckoutResponse::from);
    }

    @Transactional(readOnly = true)
    public AbandonedCheckoutResponse getAbandonedCheckoutById(UUID id) {
        AbandonedCheckout checkout = abandonedCheckoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abandoned checkout not found: " + id));
        return AbandonedCheckoutResponse.from(checkout);
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getOrderStats() {
        long totalOrders = orderRepository.count();
        long unfulfilled = orderRepository.countByFulfillmentStatus("UNFULFILLED");
        long partiallyFulfilled = orderRepository.countByFulfillmentStatus("PARTIALLY_FULFILLED");
        long fulfilled = orderRepository.countByFulfillmentStatus("FULFILLED");
        long paid = orderRepository.countByFinancialStatus("PAID");
        long pending = orderRepository.countByFinancialStatus("PENDING");
        BigDecimal totalSales = orderRepository.sumTotalSales();

        long abandonedCount = abandonedCheckoutRepository.countByRecoveryStatus("ABANDONED");
        long recoveredCount = abandonedCheckoutRepository.countByRecoveryStatus("RECOVERED");
        BigDecimal totalAbandonedValue = abandonedCheckoutRepository.sumAbandonedTotal();

        return new OrderStatsResponse(
                totalOrders,
                unfulfilled,
                partiallyFulfilled,
                fulfilled,
                paid,
                pending,
                totalSales,
                abandonedCount,
                recoveredCount,
                totalAbandonedValue
        );
    }
}
''')

write_file('order/controller/OrderController.java', '''package com.shopai.order.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.order.dto.OrderResponse;
import com.shopai.order.dto.OrderStatsResponse;
import com.shopai.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String financialStatus,
            @RequestParam(required = false) String fulfillmentStatus
    ) {
        Page<OrderResponse> orders = orderService.getOrders(page, size, search, financialStatus, fulfillmentStatus);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<OrderStatsResponse>> getOrderStats() {
        OrderStatsResponse stats = orderService.getOrderStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
''')

write_file('order/controller/AbandonedCheckoutController.java', '''package com.shopai.order.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.order.dto.AbandonedCheckoutResponse;
import com.shopai.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/abandoned-checkouts")
public class AbandonedCheckoutController {

    private final OrderService orderService;

    public AbandonedCheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<Page<AbandonedCheckoutResponse>>> getAbandonedCheckouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String recoveryStatus
    ) {
        Page<AbandonedCheckoutResponse> checkouts = orderService.getAbandonedCheckouts(page, size, search, recoveryStatus);
        return ResponseEntity.ok(ApiResponse.ok(checkouts));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<AbandonedCheckoutResponse>> getAbandonedCheckoutById(@PathVariable UUID id) {
        AbandonedCheckoutResponse checkout = orderService.getAbandonedCheckoutById(id);
        return ResponseEntity.ok(ApiResponse.ok(checkout));
    }
}
''')

# ==============================================================================
# 3. SYNC DOMAIN, REPOSITORY, SERVICES & CONTROLLER
# ==============================================================================

write_file('sync/domain/SyncJob.java', '''package com.shopai.sync.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_jobs")
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType; // CATALOG_SYNC, ORDER_SYNC, CHECKOUT_SYNC

    @Column(nullable = false, length = 50)
    private String status = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED, FAILED

    @Column(name = "items_processed", nullable = false)
    private Integer itemsProcessed = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public SyncJob() {}

    public SyncJob(String jobType) {
        this.jobType = jobType;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getItemsProcessed() { return itemsProcessed; }
    public void setItemsProcessed(Integer itemsProcessed) { this.itemsProcessed = itemsProcessed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
''')

write_file('sync/repository/SyncJobRepository.java', '''package com.shopai.sync.repository;

import com.shopai.sync.domain.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
    Optional<SyncJob> findTopByJobTypeOrderByStartedAtDesc(String jobType);
    List<SyncJob> findTop10ByOrderByStartedAtDesc();
}
''')

write_file('sync/dto/SyncStatusResponse.java', '''package com.shopai.sync.dto;

import com.shopai.sync.domain.SyncJob;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncStatusResponse(
        UUID id,
        String jobType,
        String status,
        Integer itemsProcessed,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        List<SyncJobSummary> recentJobs
) {
    public record SyncJobSummary(
            UUID id,
            String jobType,
            String status,
            Integer itemsProcessed,
            Instant startedAt,
            Instant completedAt
    ) {
        public static SyncJobSummary from(SyncJob j) {
            return new SyncJobSummary(
                    j.getId(),
                    j.getJobType(),
                    j.getStatus(),
                    j.getItemsProcessed(),
                    j.getStartedAt(),
                    j.getCompletedAt()
            );
        }
    }

    public static SyncStatusResponse from(SyncJob current, List<SyncJob> recent) {
        List<SyncJobSummary> summaries = recent != null
                ? recent.stream().map(SyncJobSummary::from).toList()
                : List.of();

        if (current == null) {
            return new SyncStatusResponse(null, "NONE", "IDLE", 0, null, null, null, summaries);
        }

        return new SyncStatusResponse(
                current.getId(),
                current.getJobType(),
                current.getStatus(),
                current.getItemsProcessed(),
                current.getErrorMessage(),
                current.getStartedAt(),
                current.getCompletedAt(),
                summaries
        );
    }
}
''')

print('Generated All Domain, Repository, DTO & Service definitions.')
