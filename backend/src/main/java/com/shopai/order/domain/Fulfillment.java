package com.shopai.order.domain;

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

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "tracking_numbers", columnDefinition = "text[]")
    private String[] trackingNumbers = new String[0];

    @Column(name = "tracking_url", columnDefinition = "TEXT")
    private String trackingUrl;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "tracking_urls", columnDefinition = "text[]")
    private String[] trackingUrls = new String[0];

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

    public String[] getTrackingNumbers() { return trackingNumbers; }
    public void setTrackingNumbers(String[] trackingNumbers) { this.trackingNumbers = trackingNumbers != null ? trackingNumbers : new String[0]; }

    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }

    public String[] getTrackingUrls() { return trackingUrls; }
    public void setTrackingUrls(String[] trackingUrls) { this.trackingUrls = trackingUrls != null ? trackingUrls : new String[0]; }

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
