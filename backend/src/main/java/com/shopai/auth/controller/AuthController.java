package com.shopai.auth.controller;

import com.shopai.auth.dto.LoginRequest;
import com.shopai.auth.dto.RegisterRequest;
import com.shopai.auth.dto.UserProfileResponse;
import com.shopai.auth.security.UserPrincipal;
import com.shopai.auth.service.AuthService;
import com.shopai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Standalone Authentication with HttpOnly Cookies")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new user (Admin only)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        UserProfileResponse profile = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(profile));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue HttpOnly access & refresh cookies")
    public ResponseEntity<ApiResponse<UserProfileResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        UserProfileResponse profile = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using HttpOnly refresh cookie")
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out and clear HttpOnly auth cookies")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile and permissions")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = authService.getCurrentProfile(principal);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
