package com.shopai.customer.dto;

import com.shopai.customer.domain.CustomerMemory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerMemoryResponse(
        UUID id,
        Long customerId,
        String customerEmail,
        String category,
        String memoryKey,
        String memoryValue,
        BigDecimal confidenceScore,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerMemoryResponse from(CustomerMemory m) {
        return new CustomerMemoryResponse(
                m.getId(),
                m.getCustomerId(),
                m.getCustomerEmail(),
                m.getCategory(),
                m.getMemoryKey(),
                m.getMemoryValue(),
                m.getConfidenceScore(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
