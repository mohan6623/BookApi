package com.marvel.springsecurity.ratelimiting;

import com.marvel.springsecurity.service.security.rateLimiting.CaffeineRateLimiter;
import com.marvel.springsecurity.service.security.rateLimiting.RateLimitConfig;
import com.marvel.springsecurity.service.security.rateLimiting.RateLimitInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Rate Limiting functionality.
 * Tests the Token Bucket algorithm implementation and endpoint configuration.
 */
class RateLimitingTest {

    private RateLimitConfig config;
    private CaffeineRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        config = new RateLimitConfig();
        rateLimiter = new CaffeineRateLimiter(config);
    }

    @Test
    @DisplayName("Rate limiter should allow requests within limit")
    void testAllowRequestsWithinLimit() {
        String testIp = "192.168.1.100";
        String endpoint = "/api/login";

        // Login limit is 10 requests per 15 minutes
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Rate limiter should block requests exceeding limit")
    void testBlockRequestsExceedingLimit() {
        String testIp = "192.168.1.101";
        String endpoint = "/api/login";

        // Use all 10 allowed requests
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowRequest(testIp, endpoint);
        }

        // 11th request should be blocked
        assertFalse(rateLimiter.allowRequest(testIp, endpoint),
                "Request exceeding limit should be blocked");
    }

    @Test
    @DisplayName("Rate limiter should track different users separately")
    void testSeparateUserTracking() {
        String endpoint = "/api/login";
        String user1 = "ip:192.168.1.1";
        String user2 = "ip:192.168.1.2";

        // Exhaust user1's limit
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowRequest(user1, endpoint);
        }
        assertFalse(rateLimiter.allowRequest(user1, endpoint), "User1 should be blocked");

        // User2 should still be allowed
        assertTrue(rateLimiter.allowRequest(user2, endpoint), "User2 should not be affected by user1's limit");
    }

    @Test
    @DisplayName("Register endpoint should have 5 requests/hour limit")
    void testRegisterEndpointLimit() {
        String testIp = "192.168.1.102";
        String endpoint = "/api/register";

        // Register limit is 5 per hour
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                    "Request " + (i + 1) + " should be allowed for register");
        }
        assertFalse(rateLimiter.allowRequest(testIp, endpoint),
                "6th register request should be blocked");
    }

    @Test
    @DisplayName("Forgot-password endpoint should be rate limited (3/hour)")
    void testForgotPasswordEndpointLimit() {
        String testIp = "192.168.1.103";
        String endpoint = "/api/forgot-password";

        // Reset password limit is 3 per hour
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                    "Request " + (i + 1) + " should be allowed for forgot-password");
        }
        assertFalse(rateLimiter.allowRequest(testIp, endpoint),
                "4th forgot-password request should be blocked");
    }

    @Test
    @DisplayName("Resend-verification endpoint should be rate limited (1/5min)")
    void testResendVerificationEndpointLimit() {
        String testIp = "192.168.1.104";
        String endpoint = "/api/resend-verification";

        // Resend limit is 1 per 5 minutes
        assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                "First resend request should be allowed");
        assertFalse(rateLimiter.allowRequest(testIp, endpoint),
                "Second resend request should be blocked (only 1/5min allowed)");
    }

    @Test
    @DisplayName("OAuth submit-email endpoint should be rate limited (5/hour)")
    void testOAuthSubmitEmailEndpointLimit() {
        String testIp = "192.168.1.105";
        String endpoint = "/api/oauth/submit-email";

        // Uses register limit: 5 per hour
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                    "Request " + (i + 1) + " should be allowed for oauth/submit-email");
        }
        assertFalse(rateLimiter.allowRequest(testIp, endpoint),
                "6th oauth/submit-email request should be blocked");
    }

    @Test
    @DisplayName("Availability check endpoint should have higher limit (20/min)")
    void testAvailabilityEndpointLimit() {
        String testIp = "192.168.1.106";
        String endpoint = "/api/available/username";

        // Available limit is 20 per minute
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                    "Request " + (i + 1) + " should be allowed for availability check");
        }
        assertFalse(rateLimiter.allowRequest(testIp, endpoint),
                "21st availability request should be blocked");
    }

    @Test
    @DisplayName("Rate limit info should return correct values")
    void testRateLimitInfo() {
        String testIp = "192.168.1.107";
        String endpoint = "/api/login";

        // Make 3 requests
        for (int i = 0; i < 3; i++) {
            rateLimiter.allowRequest(testIp, endpoint);
        }

        RateLimitInfo info = rateLimiter.getLimitInfo(testIp, endpoint);
        assertEquals(10, info.limit(), "Login endpoint limit should be 10");
        assertEquals(7, info.remaining(), "Should have 7 requests remaining after 3 used");
        assertEquals(endpoint, info.endpoint(), "Endpoint should match");
    }

    @Test
    @DisplayName("Non-rate-limited endpoint should always be allowed")
    void testNonRateLimitedEndpoint() {
        String testIp = "192.168.1.108";
        String endpoint = "/api/books"; // Not in rate limit config

        // Should always return true for non-configured endpoints
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                    "Non-rate-limited endpoint should always be allowed");
        }
    }

    @Test
    @DisplayName("RateLimitConfig should map forgot-password endpoint correctly")
    void testConfigMappingForgotPassword() {
        RateLimitConfig.EndpointLimit limit = config.getConfigForEndpoint("/api/forgot-password");
        assertNotNull(limit, "forgot-password should have a rate limit config");
        assertEquals(3, limit.getRequests(), "forgot-password should allow 3 requests");
        assertEquals(3600, limit.getWindowSeconds(), "forgot-password window should be 1 hour");
    }

    @Test
    @DisplayName("RateLimitConfig should map resend-verification endpoint correctly")
    void testConfigMappingResendVerification() {
        RateLimitConfig.EndpointLimit limit = config.getConfigForEndpoint("/api/resend-verification");
        assertNotNull(limit, "resend-verification should have a rate limit config");
        assertEquals(1, limit.getRequests(), "resend-verification should allow 1 request");
        assertEquals(300, limit.getWindowSeconds(), "resend-verification window should be 5 minutes");
    }

    @Test
    @DisplayName("RateLimitConfig should map oauth/submit-email endpoint correctly")
    void testConfigMappingOAuthSubmitEmail() {
        RateLimitConfig.EndpointLimit limit = config.getConfigForEndpoint("/api/oauth/submit-email");
        assertNotNull(limit, "oauth/submit-email should have a rate limit config");
        assertEquals(5, limit.getRequests(), "oauth/submit-email should allow 5 requests");
        assertEquals(3600, limit.getWindowSeconds(), "oauth/submit-email window should be 1 hour");
    }

    @Test
    @DisplayName("Reset limit should clear user's rate limit state")
    void testResetLimit() {
        String testIp = "192.168.1.109";
        String endpoint = "/api/login";

        // Exhaust the limit
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowRequest(testIp, endpoint);
        }
        assertFalse(rateLimiter.allowRequest(testIp, endpoint), "Should be blocked");

        // Reset the limit
        rateLimiter.resetLimit(testIp);

        // Should be allowed again after reset
        assertTrue(rateLimiter.allowRequest(testIp, endpoint),
                "Should be allowed after reset");
    }
}
