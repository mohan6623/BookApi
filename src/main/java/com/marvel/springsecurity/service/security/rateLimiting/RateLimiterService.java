package com.marvel.springsecurity.service.security.rateLimiting;

public interface RateLimiterService {
    boolean allowRequest(String key, String endpoint);
    RateLimitInfo getLimitInfo(String key, String endpoint);
    void resetLimit(String key);
}
