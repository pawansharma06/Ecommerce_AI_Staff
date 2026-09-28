package com.shopai.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CookieUtils cookieUtils;

    public JwtAuthenticationFilter(JwtService jwtService, CookieUtils cookieUtils) {
        this.jwtService = jwtService;
        this.cookieUtils = cookieUtils;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> tokenOpt = extractToken(request);

        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            Optional<Claims> claimsOpt = jwtService.validateAndExtractClaims(token);

            if (claimsOpt.isPresent()) {
                Claims claims = claimsOpt.get();
                String tokenType = claims.get("type", String.class);

                if ("ACCESS".equals(tokenType)) {
                    UUID userId = UUID.fromString(claims.getSubject());
                    String email = claims.get("email", String.class);

                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    @SuppressWarnings("unchecked")
                    List<String> permissions = claims.get("permissions", List.class);

                    Set<SimpleGrantedAuthority> authorities = new HashSet<>();
                    if (roles != null) {
                        for (String role : roles) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                            authorities.add(new SimpleGrantedAuthority(role));
                        }
                    }
                    if (permissions != null) {
                        for (String perm : permissions) {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                    }

                    UserPrincipal principal = new UserPrincipal(
                            userId,
                            email,
                            "",
                            email,
                            true,
                            authorities
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        // Priority 1: HttpOnly Cookie (Primary for frontend SPA)
        Optional<String> cookieToken = cookieUtils.extractTokenFromCookie(request, CookieUtils.ACCESS_TOKEN_COOKIE);
        if (cookieToken.isPresent()) {
            return cookieToken;
        }

        // Priority 2: Authorization: Bearer <token> (For API clients & integrations)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }

        return Optional.empty();
    }
}
