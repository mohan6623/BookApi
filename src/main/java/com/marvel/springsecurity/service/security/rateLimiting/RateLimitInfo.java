package com.marvel.springsecurity.service.security.rateLimiting;

public record RateLimitInfo(
        int remaining,
        long resetTimeEpochSeconds,
        int limit,
        String endpoint
) {

    public long retryAfterSeconds(){
        long now = System.currentTimeMillis() / 1000;
        return Math.max(0, resetTimeEpochSeconds - now);
    }

    public boolean isExtended(){
        return remaining <= 0;
    }
}
