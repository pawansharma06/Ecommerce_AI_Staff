package com.shopai.auth.controller;

import com.shopai.auth.dto.CreateRoleRequest;
import com.shopai.auth.dto.PermissionResponse;
import com.shopai.auth.dto.RoleResponse;
import com.shopai.auth.service.RoleService;
import com.shopai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles & Permissions", description = "RBAC Role and Permission management")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        List<RoleResponse> roles = roleService.getRoles();
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create custom role")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(role));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all available system permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        List<PermissionResponse> permissions = roleService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }
}
