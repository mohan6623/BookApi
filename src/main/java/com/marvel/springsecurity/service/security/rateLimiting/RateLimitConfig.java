package com.marvel.springsecurity.service.security.rateLimiting;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for rate limiting.
 * Maps endpoints to their specific rate limit rules.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitConfig {

    private boolean enabled = true;
    private boolean includeHeaders = true;

    // Cache settings
    private CacheSettings cache = new CacheSettings();

    // Endpoint-specific configurations (defaults shown)
    private EndpointLimit register = new EndpointLimit(5, 3600); // 5 per hour
    private EndpointLimit login = new EndpointLimit(10, 900); // 10 per 15 minutes
    private EndpointLimit verify = new EndpointLimit(5, 3600); // 5 per hour
    private EndpointLimit resend = new EndpointLimit(1, 300); // 1 per 5 minutes
    private EndpointLimit resetPassword = new EndpointLimit(3, 3600); // 3 per hour
    private EndpointLimit available = new EndpointLimit(20, 60); // 20 per minute
    private EndpointLimit oauth = new EndpointLimit(10, 3600); // 10 per hour
    private EndpointLimit userUpdate = new EndpointLimit(30, 3600); // 30 per hour (general updates)
    private EndpointLimit fileUpload = new EndpointLimit(10, 3600); // 10 per hour (heavy resources)

    @Data
    public static class CacheSettings {
        private int maxSize = 10000;
        private int expireHours = 2;
    }

    @Data
    public static class EndpointLimit {
        private int requests;
        private int windowSeconds;

        public EndpointLimit() {
        }

        public EndpointLimit(int requests, int windowSeconds) {
            this.requests = requests;
            this.windowSeconds = windowSeconds;
        }
    }

    /**
     * Get rate limit configuration for a specific endpoint.
     * Returns null if endpoint is not rate limited.
     */
    public EndpointLimit getConfigForEndpoint(String endpoint) {
        if (endpoint == null)
            return null;

        // Normalize endpoint (remove /api prefix if present)
        String normalizedEndpoint = endpoint.startsWith("/api")
                ? endpoint.substring(4)
                : endpoint;

        // Map endpoints to configurations
        Map<String, EndpointLimit> endpointMap = new HashMap<>();
        endpointMap.put("/register", register);
        endpointMap.put("/login", login);
        endpointMap.put("/register/verify-email", verify);
        endpointMap.put("/register/resend-verification", resend);
        endpointMap.put("/update/reset-password", resetPassword);
        endpointMap.put("/available/username", available);
        endpointMap.put("/available/mail", available);
        endpointMap.put("/oauth/callback", oauth);
        endpointMap.put("/validate/verify-email", verify);
        endpointMap.put("/validate/forgot-password", resetPassword);
        endpointMap.put("/forgot-password", resetPassword); // Password reset request - 3/hr
        endpointMap.put("/resend-verification", resend); // Email resend - 1/5min
        endpointMap.put("/oauth/submit-email", register); // OAuth email - 5/hr

        // User Profile Updates
        endpointMap.put("/user/update-profile-pic", fileUpload); // Heavy resource - 10/hr
        endpointMap.put("/user/update-password", resetPassword); // Sensitive - match reset limits (3/hr)
        endpointMap.put("/user", userUpdate); // Catch-all for other /user endpoints (update name, username)

        // Check for exact match
        if (endpointMap.containsKey(normalizedEndpoint)) {
            return endpointMap.get(normalizedEndpoint);
        }

        // Check for partial matches (e.g., /register/* matches /register)
        for (Map.Entry<String, EndpointLimit> entry : endpointMap.entrySet()) {
            if (normalizedEndpoint.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null; // No rate limit for this endpoint
    }
}