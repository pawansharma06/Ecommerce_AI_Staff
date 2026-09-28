package com.shopai.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    // In-memory fallback if Redis is unavailable
    private final ConcurrentHashMap<String, InMemoryBucket> fallbackMap = new ConcurrentHashMap<>();

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public record RateLimitResult(
            boolean allowed,
            long limit,
            long remaining,
            long retryAfterSeconds
    ) {}

    public RateLimitResult tryAcquire(String key, int maxRequestsPerWindow, int windowSeconds) {
        String redisKey = "ratelimit:" + key;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                Long currentCount = redisTemplate.opsForValue().increment(redisKey);
                if (currentCount != null && currentCount == 1) {
                    redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
                }

                long count = currentCount != null ? currentCount : 1;
                long remaining = Math.max(0, maxRequestsPerWindow - count);
                boolean allowed = count <= maxRequestsPerWindow;
                Long ttl = redisTemplate.getExpire(redisKey);
                long retryAfter = ttl != null && ttl > 0 ? ttl : windowSeconds;

                return new RateLimitResult(allowed, maxRequestsPerWindow, remaining, allowed ? 0 : retryAfter);
            }
        } catch (Exception e) {
            log.warn("Redis rate limiter fallback to in-memory: {}", e.getMessage());
        }

        // In-memory fallback
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;
        InMemoryBucket bucket = fallbackMap.compute(key, (k, b) -> {
            if (b == null || now - b.windowStart > windowMillis) {
                return new InMemoryBucket(now, new AtomicInteger(1));
            }
            b.counter.incrementAndGet();
            return b;
        });

        int count = bucket.counter.get();
        long remaining = Math.max(0, maxRequestsPerWindow - count);
        boolean allowed = count <= maxRequestsPerWindow;
        long elapsed = now - bucket.windowStart;
        long retryAfter = Math.max(1, (windowMillis - elapsed) / 1000L);

        return new RateLimitResult(allowed, maxRequestsPerWindow, remaining, allowed ? 0 : retryAfter);
    }

    private record InMemoryBucket(long windowStart, AtomicInteger counter) {}
}
