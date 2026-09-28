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
import com.shopai.common.exception.ShopAiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private JwtService jwtService;
    @Mock private CookieUtils cookieUtils;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                jwtService,
                cookieUtils,
                passwordEncoder
        );
    }

    @Test
    void registersUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "owner@acme.com",
                "Password123!",
                "John",
                "Doe"
        );

        Role adminRole = new Role("ADMIN", "Admin Role", true);
        adminRole.setPermissions(Set.of(new Permission("product.read", "Read products", "product")));

        when(userRepository.existsByEmail("owner@acme.com")).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_pwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        UserProfileResponse profile = authService.register(request, response);

        assertThat(profile).isNotNull();
        assertThat(profile.email()).isEqualTo("owner@acme.com");
        assertThat(profile.roles()).contains("ADMIN");
        assertThat(profile.permissions()).contains("product.read");

        verify(cookieUtils).addAccessTokenCookie(eq(response), eq("access_token"), anyLong());
        verify(cookieUtils).addRefreshTokenCookie(eq(response), eq("refresh_token"), anyLong());
    }

    @Test
    void rejectsLoginWithInvalidPassword() {
        LoginRequest request = new LoginRequest("owner@acme.com", "wrong_password");
        User user = new User("owner@acme.com", "correct_hash", "John", "Doe");

        when(userRepository.findByEmail("owner@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "correct_hash")).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.login(request, response))
                .isInstanceOf(ShopAiException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
