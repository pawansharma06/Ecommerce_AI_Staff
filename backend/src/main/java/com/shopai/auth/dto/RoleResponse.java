package com.shopai.auth.dto;

import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        boolean isSystem,
        Set<String> permissions
) {}
