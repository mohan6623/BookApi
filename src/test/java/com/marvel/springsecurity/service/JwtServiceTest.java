package com.marvel.springsecurity.service;

import com.marvel.springsecurity.service.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for JwtService.
 * Tests token generation, validation, extraction, and security scenarios.
 */
class JwtServiceTest {

    private JwtService jwtService;

    // Test constants
    private static final String TEST_SECRET = "e3f7a9c4b1d2e8f9257a6c4b3d2e1f0a4b6c8d9e0f1a2b3c4d5e6f798a1b2cd4";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ROLE = "ROLE_USER";
    private static final int TEST_USER_ID = 123;
    private static final int TEST_ROLE_VERSION = 1;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject test values using reflection
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1800000L); // 30 min
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days
    }

    // ==================== TOKEN GENERATION TESTS ====================

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid auth token with username and role")
        void testGenerateTokenWithUsernameAndRole() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals(TEST_USERNAME, jwtService.extractUserName(token));
        }

        @Test
        @DisplayName("Should generate token with all claims (username, role, roleVersion, userId)")
        void testGenerateTokenWithAllClaims() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE, TEST_ROLE_VERSION, TEST_USER_ID);

            assertNotNull(token);
            Claims claims = jwtService.extractAllClaims(token);
            assertEquals(TEST_USERNAME, claims.getSubject());
            assertEquals(TEST_ROLE, claims.get("role"));
            assertEquals(TEST_ROLE_VERSION, claims.get("roleVersion"));
            assertEquals(TEST_USER_ID, claims.get("userId"));
        }

        @Test
        @DisplayName("Should generate token with image URL when provided")
        void testGenerateTokenWithImageUrl() {
            String imageUrl = "https://example.com/image.jpg";
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE, TEST_ROLE_VERSION, TEST_USER_ID,
                    imageUrl);

            Claims claims = jwtService.extractAllClaims(token);
            assertEquals(imageUrl, claims.get("imageUrl"));
        }

        @Test
        @DisplayName("Should generate email verification token with correct type")
        void testGenerateEmailToken() {
            String token = jwtService.generateEmailToken(TEST_EMAIL);

            assertNotNull(token);
            Claims claims = jwtService.extractAllClaims(token);
            assertEquals(TEST_EMAIL, claims.getSubject());
            assertEquals("email_verification", claims.get("type"));
        }

        @Test
        @DisplayName("Should generate password reset token with correct type")
        void testGeneratePasswordResetToken() {
            String token = jwtService.generatePasswordResetToken(TEST_EMAIL);

            assertNotNull(token);
            Claims claims = jwtService.extractAllClaims(token);
            assertEquals(TEST_EMAIL, claims.getSubject());
            assertEquals("password_reset", claims.get("type"));
        }

        @Test
        @DisplayName("Should generate refresh token with correct type")
        void testGenerateRefreshToken() {
            String token = jwtService.generateRefreshToken(TEST_USERNAME);

            assertNotNull(token);
            Claims claims = jwtService.extractAllClaims(token);
            assertEquals(TEST_USERNAME, claims.getSubject());
            assertEquals("refresh", claims.get("type"));
        }

        @Test
        @DisplayName("Should generate OAuth pending token with all provider data")
        void testGenerateOAuthPendingToken() {
            String providerId = "12345";
            String name = "Test User";
            String picture = "https://example.com/pic.jpg";
            String provider = "GOOGLE";

            String token = jwtService.generateOAuthPendingToken(providerId, name, picture, provider);

            assertNotNull(token);
            Claims claims = jwtService.extractAllClaims(token);
            assertEquals("oauth_pending", claims.get("type"));
            assertEquals(providerId, claims.get("providerId"));
            assertEquals(name, claims.get("name"));
            assertEquals(picture, claims.get("picture"));
            assertEquals(provider, claims.get("provider"));
        }
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate unexpired token as valid")
        void testValidateValidToken() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE);

            assertTrue(jwtService.validateToken(token));
        }

        @Test
        @DisplayName("Should throw ExpiredJwtException for expired token validation")
        void testValidateExpiredToken() {
            // Create a service with very short expiration
            JwtService shortLivedService = new JwtService();
            ReflectionTestUtils.setField(shortLivedService, "secretKey", TEST_SECRET);
            ReflectionTestUtils.setField(shortLivedService, "jwtExpiration", 1L); // 1ms

            String token = shortLivedService.generateToken(TEST_USERNAME, TEST_ROLE);

            // Wait for token to expire
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }

            // validateToken throws ExpiredJwtException when parsing expired tokens
            assertThrows(ExpiredJwtException.class, () -> shortLivedService.validateToken(token));
        }

        @Test
        @DisplayName("Should throw exception for malformed token")
        void testMalformedToken() {
            String malformedToken = "not.a.valid.jwt.token";

            assertThrows(MalformedJwtException.class, () -> jwtService.extractAllClaims(malformedToken));
        }

        @Test
        @DisplayName("Should throw exception for token with invalid signature")
        void testInvalidSignatureToken() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE);
            // Tamper with the token by changing the last character
            String tamperedToken = token.substring(0, token.length() - 1) + "X";

            assertThrows(SignatureException.class, () -> jwtService.extractAllClaims(tamperedToken));
        }

        @Test
        @DisplayName("Should throw exception for completely invalid token")
        void testCompletelyInvalidToken() {
            assertThrows(Exception.class, () -> jwtService.extractAllClaims("invalid"));
        }
    }

    // ==================== TOKEN EXTRACTION TESTS ====================

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract username from token")
        void testExtractUserName() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE);

            assertEquals(TEST_USERNAME, jwtService.extractUserName(token));
        }

        @Test
        @DisplayName("Should extract email from valid token")
        void testExtractEmail() {
            String token = jwtService.generateEmailToken(TEST_EMAIL);

            assertEquals(TEST_EMAIL, jwtService.extractEmail(token));
        }

        @Test
        @DisplayName("Should return null for invalid email token")
        void testExtractEmailFromInvalidToken() {
            // extractEmail returns null for invalid tokens (catches exception internally)
            assertNull(jwtService.extractEmail("invalid.token.here"));
        }

        @Test
        @DisplayName("Should extract email from password reset token")
        void testExtractEmailFromResetToken() {
            String token = jwtService.generatePasswordResetToken(TEST_EMAIL);

            assertEquals(TEST_EMAIL, jwtService.extractEmailFromResetToken(token));
        }

        @Test
        @DisplayName("Should return null when extracting email from non-reset token")
        void testExtractEmailFromNonResetToken() {
            String token = jwtService.generateEmailToken(TEST_EMAIL);

            // Should return null because type is not 'password_reset'
            assertNull(jwtService.extractEmailFromResetToken(token));
        }

        @Test
        @DisplayName("Should extract OAuth pending data correctly")
        void testExtractOAuthPendingData() {
            String providerId = "12345";
            String name = "Test User";
            String picture = "https://example.com/pic.jpg";
            String provider = "GITHUB";

            String token = jwtService.generateOAuthPendingToken(providerId, name, picture, provider);
            Map<String, String> data = jwtService.extractOAuthPendingData(token);

            assertNotNull(data);
            assertEquals(providerId, data.get("providerId"));
            assertEquals(name, data.get("name"));
            assertEquals(picture, data.get("picture"));
            assertEquals(provider, data.get("provider"));
        }

        @Test
        @DisplayName("Should return null for non-OAuth pending token")
        void testExtractOAuthDataFromWrongTokenType() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE);

            assertNull(jwtService.extractOAuthPendingData(token));
        }

        @Test
        @DisplayName("Should extract all claims correctly")
        void testExtractAllClaims() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE, TEST_ROLE_VERSION, TEST_USER_ID);

            Claims claims = jwtService.extractAllClaims(token);

            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());
            assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
        }
    }

    // ==================== SECURITY VULNERABILITY TESTS ====================

    @Nested
    @DisplayName("Security Vulnerability Tests")
    class SecurityVulnerabilityTests {

        @Test
        @DisplayName("Should reject token signed with different secret")
        void testRejectDifferentSecretToken() {
            // Create token with different secret
            JwtService otherService = new JwtService();
            ReflectionTestUtils.setField(otherService, "secretKey",
                    "different_secret_key_that_is_long_enough_for_256_bits");
            ReflectionTestUtils.setField(otherService, "jwtExpiration", 1800000L);

            String token = otherService.generateToken(TEST_USERNAME, TEST_ROLE);

            // Should fail signature verification
            assertThrows(SignatureException.class, () -> jwtService.extractAllClaims(token));
        }

        @Test
        @DisplayName("Should handle null token gracefully")
        void testNullTokenHandling() {
            assertThrows(Exception.class, () -> jwtService.extractAllClaims(null));
        }

        @Test
        @DisplayName("Should handle empty token gracefully")
        void testEmptyTokenHandling() {
            assertThrows(Exception.class, () -> jwtService.extractAllClaims(""));
        }

        @Test
        @DisplayName("Should reject token with modified claims")
        void testModifiedClaimsRejection() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE);

            // Try to modify the payload (middle part of JWT)
            String[] parts = token.split("\\.");
            parts[1] = parts[1] + "modified";
            String tamperedToken = String.join(".", parts);

            assertThrows(Exception.class, () -> jwtService.extractAllClaims(tamperedToken));
        }

        @Test
        @DisplayName("Token should have reasonable expiration time")
        void testTokenExpirationTime() {
            String token = jwtService.generateToken(TEST_USERNAME, TEST_ROLE);
            Claims claims = jwtService.extractAllClaims(token);

            long issuedAt = claims.getIssuedAt().getTime();
            long expiration = claims.getExpiration().getTime();
            long expectedDuration = 1800000L; // 30 minutes

            // Allow 1 second tolerance
            assertTrue(Math.abs((expiration - issuedAt) - expectedDuration) < 1000);
        }
    }
}
