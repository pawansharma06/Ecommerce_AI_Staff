package com.shopai.order.dto;

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
