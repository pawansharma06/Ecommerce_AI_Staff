package com.shopai.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "9a4f2c8d3e7b1a5f6c8d2e4a7b9c1d3e5f8a0b2c4d6e8f1a3b5c7d9e1f2a4b6c";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 3600, 604800);
    }

    @Test
    void generatesAndValidatesAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "admin@shopai.dev";
        List<String> roles = List.of("ADMIN");
        List<String> permissions = List.of("product.read", "product.create", "order.read");

        String token = jwtService.generateAccessToken(userId, email, roles, permissions);

        assertThat(token).isNotBlank();

        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);
        assertThat(claimsOpt).isPresent();

        Claims claims = claimsOpt.get();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");

        @SuppressWarnings("unchecked")
        List<String> extractedRoles = claims.get("roles", List.class);
        assertThat(extractedRoles).containsExactlyElementsOf(roles);

        @SuppressWarnings("unchecked")
        List<String> extractedPerms = claims.get("permissions", List.class);
        assertThat(extractedPerms).containsExactlyInAnyOrderElementsOf(permissions);
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(userId);

        assertThat(token).isNotBlank();

        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);
        assertThat(claimsOpt).isPresent();

        Claims claims = claimsOpt.get();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("type", String.class)).isEqualTo("REFRESH");
    }

    @Test
    void returnsEmptyForTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "test@shopai.dev", List.of("product.read"));

        String tamperedToken = token + "xyz";

        Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(tamperedToken);
        assertThat(claimsOpt).isEmpty();
    }
}
