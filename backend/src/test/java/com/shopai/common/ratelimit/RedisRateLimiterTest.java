package com.shopai.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiter(redisTemplate);
    }

    @Test
    void shouldAllowRequestWithinLimit() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("ratelimit:test_key")).thenReturn(1L);
        when(redisTemplate.getExpire("ratelimit:test_key")).thenReturn(59L);

        RedisRateLimiter.RateLimitResult result = rateLimiter.tryAcquire("test_key", 10, 60);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.remaining()).isEqualTo(9);
        verify(redisTemplate).expire(eq("ratelimit:test_key"), any(Duration.class));
    }

    @Test
    void shouldBlockRequestExceedingLimit() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("ratelimit:exceeded_key")).thenReturn(11L);
        when(redisTemplate.getExpire("ratelimit:exceeded_key")).thenReturn(45L);

        RedisRateLimiter.RateLimitResult result = rateLimiter.tryAcquire("exceeded_key", 10, 60);

        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isEqualTo(0);
        assertThat(result.retryAfterSeconds()).isEqualTo(45L);
    }

    @Test
    void shouldFallbackToInMemoryWhenRedisFails() {
        when(redisTemplate.getConnectionFactory()).thenThrow(new RuntimeException("Redis unreachable"));

        RedisRateLimiter.RateLimitResult res1 = rateLimiter.tryAcquire("in_memory_key", 2, 60);
        assertThat(res1.allowed()).isTrue();
        assertThat(res1.remaining()).isEqualTo(1);

        RedisRateLimiter.RateLimitResult res2 = rateLimiter.tryAcquire("in_memory_key", 2, 60);
        assertThat(res2.allowed()).isTrue();
        assertThat(res2.remaining()).isEqualTo(0);

        RedisRateLimiter.RateLimitResult res3 = rateLimiter.tryAcquire("in_memory_key", 2, 60);
        assertThat(res3.allowed()).isFalse();
    }
}
