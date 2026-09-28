package com.shopai.order.domain;

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

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "shipping_address", columnDefinition = "jsonb")
    private String shippingAddress;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private String billingAddress;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags = new String[0];

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

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags != null ? tags : new String[0]; }

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
