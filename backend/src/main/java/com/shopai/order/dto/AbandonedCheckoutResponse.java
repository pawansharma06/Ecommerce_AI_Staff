package com.shopai.order.dto;

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
