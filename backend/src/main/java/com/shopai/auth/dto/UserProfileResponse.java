package com.shopai.auth.dto;

import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String status,
        Set<String> roles,
        Set<String> permissions
) {}
