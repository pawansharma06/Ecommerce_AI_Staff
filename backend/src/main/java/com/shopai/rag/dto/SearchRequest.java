package com.shopai.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank(message = "Search query cannot be blank")
        String query,
        String customerEmail,
        String documentType,
        Double minScore,
        Integer limit
) {}
