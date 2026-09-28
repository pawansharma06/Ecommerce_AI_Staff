package com.shopai.order.dto;

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
