package com.shopai.order.dto;

import com.shopai.order.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
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
        List<String> tags,
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

        List<String> tagList = o.getTags() != null ? Arrays.asList(o.getTags()) : List.of();

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
                tagList,
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
