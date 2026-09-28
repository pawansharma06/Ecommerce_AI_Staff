package com.shopai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateRoleRequest(
        @NotBlank(message = "Role name is required")
        @Size(min = 2, max = 100)
        String name,

        String description,
        Set<String> permissionNames
) {}
