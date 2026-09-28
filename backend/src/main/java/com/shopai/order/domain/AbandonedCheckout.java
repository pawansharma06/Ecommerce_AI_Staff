package com.shopai.order.domain;

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

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
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
