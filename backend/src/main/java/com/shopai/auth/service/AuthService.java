package com.shopai.auth.service;

import com.shopai.auth.domain.Permission;
import com.shopai.auth.domain.Role;
import com.shopai.auth.domain.User;
import com.shopai.auth.dto.LoginRequest;
import com.shopai.auth.dto.RegisterRequest;
import com.shopai.auth.dto.UserProfileResponse;
import com.shopai.auth.repository.RoleRepository;
import com.shopai.auth.repository.UserRepository;
import com.shopai.auth.security.CookieUtils;
import com.shopai.auth.security.JwtService;
import com.shopai.auth.security.UserPrincipal;
import com.shopai.common.exception.ResourceNotFoundException;
import com.shopai.common.exception.ShopAiException;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtService jwtService,
            CookieUtils cookieUtils,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.cookieUtils = cookieUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void initDefaultAdmin() {
        if (userRepository.count() == 0) {
            log.info("Bootstrapping default standalone administrator (admin@shopai.dev)...");
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN", "System Administrator", true)));

            User admin = new User(
                    "admin@shopai.dev",
                    passwordEncoder.encode("AdminPassword123!"),
                    "System",
                    "Administrator"
            );
            admin.setRoles(new HashSet<>(List.of(adminRole)));
            userRepository.save(admin);
            log.info("Default administrator initialized successfully.");
        }
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request, HttpServletResponse response) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ShopAiException("USER_EXISTS", "User with email '" + email + "' already exists", HttpStatus.CONFLICT);
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName()
        );
        user.setRoles(new HashSet<>(List.of(adminRole)));
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);

        issueAuthCookies(user, response);
        return buildProfile(user);
    }

    @Transactional
    public UserProfileResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ShopAiException("INVALID_CREDENTIALS", "Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ShopAiException("ACCOUNT_INACTIVE", "Your account is inactive", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ShopAiException("INVALID_CREDENTIALS", "Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        issueAuthCookies(user, response);
        return buildProfile(user);
    }

    @Transactional
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtils.extractTokenFromCookie(request, CookieUtils.REFRESH_TOKEN_COOKIE)
                .orElseThrow(() -> new ShopAiException("NO_REFRESH_TOKEN", "Refresh token missing", HttpStatus.UNAUTHORIZED));

        Claims claims = jwtService.validateAndExtractClaims(refreshToken)
                .orElseThrow(() -> new ShopAiException("INVALID_REFRESH_TOKEN", "Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (!"REFRESH".equals(claims.get("type", String.class))) {
            throw new ShopAiException("INVALID_TOKEN_TYPE", "Token is not a refresh token", HttpStatus.UNAUTHORIZED);
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ShopAiException("USER_NOT_FOUND", "User not found", HttpStatus.UNAUTHORIZED));

        issueAuthCookies(user, response);
    }

    public void logout(HttpServletResponse response) {
        cookieUtils.clearAuthCookies(response);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId().toString()));
        return buildProfile(user);
    }

    private void issueAuthCookies(User user, HttpServletResponse response) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        Set<String> permissions = extractPermissions(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roleNames,
                permissions
        );

        String refreshToken = jwtService.generateRefreshToken(user.getId());

        cookieUtils.addAccessTokenCookie(response, accessToken, jwtService.getExpirationSeconds());
        cookieUtils.addRefreshTokenCookie(response, refreshToken, jwtService.getRefreshExpirationSeconds());
    }

    private UserProfileResponse buildProfile(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = extractPermissions(user);

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                roleNames,
                permissions
        );
    }

    private Set<String> extractPermissions(User user) {
        return user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}
