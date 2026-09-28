package com.shopai.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequest(
        @NotBlank(message = "Title cannot be blank")
        String title,

        @NotBlank(message = "Content cannot be blank")
        String content,

        String documentType, // POLICY, FAQ, CUSTOM
        String customerEmail
) {}
