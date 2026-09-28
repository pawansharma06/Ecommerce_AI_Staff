package com.shopai.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(1) // Run before security filter
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RedisRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RedisRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        int limit = -1;
        int window = 60;
        String rateLimitKey = null;

        if (path.startsWith("/api/v1/auth/login")) {
            limit = 20; // 20 login attempts per minute per IP
            rateLimitKey = "auth_login:" + clientIp;
        } else if (path.startsWith("/api/v1/shopify/webhooks") || path.contains("/webhook")) {
            limit = 300; // 300 webhook events per minute
            rateLimitKey = "webhook_ingress:" + clientIp;
        } else if (path.contains("/conversations/") && path.endsWith("/messages")) {
            limit = 60; // 60 messages per minute per IP
            rateLimitKey = "agent_messages:" + clientIp;
        }

        if (limit > 0 && rateLimitKey != null) {
            RedisRateLimiter.RateLimitResult result = rateLimiter.tryAcquire(rateLimitKey, limit, window);

            response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

            if (!result.allowed()) {
                log.warn("Rate limit exceeded for client IP [{}] on endpoint [{}] (Retry in {}s)", clientIp, path, result.retryAfterSeconds());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));

                Map<String, Object> errorBody = Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "TOO_MANY_REQUESTS",
                                "message", "Rate limit exceeded. Please retry after " + result.retryAfterSeconds() + " seconds."
                        ),
                        "retryAfterSeconds", result.retryAfterSeconds()
                );
                response.getWriter().write(objectMapper.writeValueAsString(errorBody));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
