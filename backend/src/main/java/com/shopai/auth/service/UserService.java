package com.shopai.auth.service;

import com.shopai.auth.domain.Role;
import com.shopai.auth.domain.User;
import com.shopai.auth.dto.CreateUserRequest;
import com.shopai.auth.dto.UserResponse;
import com.shopai.auth.repository.RoleRepository;
import com.shopai.auth.repository.UserRepository;
import com.shopai.common.exception.ResourceNotFoundException;
import com.shopai.common.exception.ShopAiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return toResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ShopAiException("USER_EXISTS",
                    "User with email '" + request.email() + "' already exists", HttpStatus.CONFLICT);
        }

        User user = new User(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName()
        );

        if (request.roleNames() != null && !request.roleNames().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.roleNames()) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(UUID userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        user.setStatus(status);
        user = userRepository.save(user);
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.isEmailVerified(),
                roleNames,
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
