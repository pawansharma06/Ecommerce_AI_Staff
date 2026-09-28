package com.shopai.auth.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String status,
        boolean emailVerified,
        Set<String> roles,
        Instant lastLoginAt,
        Instant createdAt
) {}
