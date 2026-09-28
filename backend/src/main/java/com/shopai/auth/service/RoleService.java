package com.shopai.auth.service;

import com.shopai.auth.domain.Permission;
import com.shopai.auth.domain.Role;
import com.shopai.auth.dto.CreateRoleRequest;
import com.shopai.auth.dto.PermissionResponse;
import com.shopai.auth.dto.RoleResponse;
import com.shopai.auth.repository.PermissionRepository;
import com.shopai.auth.repository.RoleRepository;
import com.shopai.common.exception.ShopAiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(p -> new PermissionResponse(p.getId(), p.getName(), p.getDescription(), p.getModule()))
                .toList();
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.findByName(request.name()).isPresent()) {
            throw new ShopAiException("ROLE_EXISTS",
                    "Role with name '" + request.name() + "' already exists", HttpStatus.CONFLICT);
        }

        Role role = new Role(request.name(), request.description(), false);

        if (request.permissionNames() != null && !request.permissionNames().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (String permName : request.permissionNames()) {
                permissionRepository.findByName(permName).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        return toResponse(role);
    }

    public RoleResponse toResponse(Role role) {
        Set<String> perms = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                perms
        );
    }
}
