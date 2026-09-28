package com.shopai.auth.dto;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String name,
        String description,
        String module
) {}
