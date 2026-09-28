package com.shopai.customer.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateCustomerMemoryRequest(
        @NotBlank(message = "Customer email cannot be blank")
        String customerEmail,

        String category, // PREFERENCE, ORDER_FACT, ADDRESS_FACT, FEEDBACK, INTERACTION

        @NotBlank(message = "Memory key cannot be blank")
        String memoryKey,

        @NotBlank(message = "Memory value cannot be blank")
        String memoryValue,

        BigDecimal confidenceScore
) {}
