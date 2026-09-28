package com.shopai.order.dto;

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
