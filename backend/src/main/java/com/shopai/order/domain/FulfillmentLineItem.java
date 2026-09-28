package com.shopai.order.domain;

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
