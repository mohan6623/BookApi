package com.marvel.springsecurity.service.security.rateLimiting;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * In-memory rate limiter using Caffeine cache with Token Bucket algorithm.
 * Thread-safe and high-performance for single-instance deployments.
 * Can be easily replaced with Redis-based implementation for multi-instance scaling.
 */
@Slf4j
@Service
@Primary
public class CaffeineRateLimiter implements RateLimiterService {

    private final Cache<String, TokenBucket> cache;
    private final RateLimitConfig config;

    public CaffeineRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.cache = Caffeine.newBuilder()
                .maximumSize(config.getCache().getMaxSize())
                .expireAfterWrite(config.getCache().getExpireHours(), TimeUnit.HOURS)
                .recordStats() // Enable statistics for monitoring
                .build();

        log.info("Caffeine rate limiter initialized with max size: {}, expiry: {} hours",
                config.getCache().getMaxSize(),
                config.getCache().getExpireHours());
    }

    @Override
    public boolean allowRequest(String key, String endpoint) {
        if (!config.isEnabled()) {
            return true; // Rate limiting disabled
        }

        RateLimitConfig.EndpointLimit endpointConfig = config.getConfigForEndpoint(endpoint);
        if (endpointConfig == null) {
            return true; // No rate limit configured for this endpoint
        }

        String cacheKey = buildCacheKey(endpoint, key);
        TokenBucket bucket = cache.get(cacheKey, k -> new TokenBucket(
                endpointConfig.getRequests(),
                endpointConfig.getWindowSeconds()
        ));

        boolean allowed = bucket.tryConsume();

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {} on endpoint: {}", key, endpoint);
        }

        return allowed;
    }

    @Override
    public RateLimitInfo getLimitInfo(String key, String endpoint) {
        RateLimitConfig.EndpointLimit endpointConfig = config.getConfigForEndpoint(endpoint);
        if (endpointConfig == null) {
            return new RateLimitInfo(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, endpoint); //initializing
        }

        String cacheKey = buildCacheKey(endpoint, key);
        TokenBucket bucket = cache.getIfPresent(cacheKey);

        if (bucket == null) {
            // No requests made yet, all tokens available
            return new RateLimitInfo(
                    endpointConfig.getRequests(),
                    System.currentTimeMillis() / 1000 + endpointConfig.getWindowSeconds(),
                    endpointConfig.getRequests(),
                    endpoint
            );
        }

        return new RateLimitInfo(
                bucket.getAvailableTokens(),
                bucket.getResetTimeEpochSeconds(),
                bucket.getCapacity(),
                endpoint
        );
    }

    @Override
    public void resetLimit(String key) {
        // Remove all entries for this key (across all endpoints)
        cache.asMap().keySet().removeIf(k -> k.endsWith(":" + key));
        log.info("Reset rate limits for key: {}", key);
    }

    /**
     * Build cache key in format: "endpoint:identifier"
     */
    private String buildCacheKey(String endpoint, String key) {
        return endpoint + ":" + key;
    }

    /**
     * Get cache statistics for monitoring.
     */
    public String getCacheStats() {
        return cache.stats().toString();
    }

    /**
     * Token Bucket implementation for smooth rate limiting.
     * Tokens refill gradually over time, allowing burst traffic while maintaining average rate.
     */
    private static class TokenBucket {
        private double tokens;
        @Getter
        private final int capacity;
        private final long windowNanos;
        private long lastRefillNanos;
        private final double tokensPerNano;

        public TokenBucket(int capacity, int windowSeconds) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.windowNanos = TimeUnit.SECONDS.toNanos(windowSeconds);
            this.lastRefillNanos = System.nanoTime();
            // Calculate how many tokens to add per nanosecond
            this.tokensPerNano = (double) capacity / windowNanos;
        }

        /**
         * Try to consume one token. Returns true if successful, false if bucket is empty.
         * Thread-safe operation.
         */
        public synchronized boolean tryConsume() {
            refill();

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }

            return false;
        }

        /**
         * Refill tokens based on elapsed time since last refill.
         */
        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillNanos;

            if (elapsedNanos > 0) {
                double tokensToAdd = elapsedNanos * tokensPerNano;
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillNanos = now;
            }
        }

        public synchronized int getAvailableTokens() {
            refill();
            return (int) Math.floor(tokens);
        }

        public long getResetTimeEpochSeconds() {
            long now = System.nanoTime();
            long nanosUntilFull = (long) ((capacity - tokens) / tokensPerNano);
            long resetTimeNanos = now + nanosUntilFull;
            return TimeUnit.NANOSECONDS.toSeconds(resetTimeNanos);
        }
    }
}