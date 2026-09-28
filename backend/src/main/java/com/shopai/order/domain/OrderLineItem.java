package com.shopai.order.domain;

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
