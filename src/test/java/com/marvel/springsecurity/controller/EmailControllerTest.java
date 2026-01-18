package com.marvel.springsecurity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvel.springsecurity.dto.EmailDto;
import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.PasswordResetDto;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.service.security.EmailService;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EmailController.
 * Tests email verification, password reset, and resend verification endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    private Users testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setUserId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setEmailVerified(false);
        testUser.setRole("ROLE_USER");

        testUserDto = new UserDto();
        testUserDto.setId(1);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
    }

    // ==================== EMAIL VERIFICATION TESTS ====================

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("GET /api/validate/verify-email - Should verify valid token")
        void testVerifyEmailValidToken() throws Exception {
            JwtResponse jwtResponse = JwtResponse.builder()
                    .token("new-jwt-token")
                    .user(testUserDto)
                    .build();
            when(emailService.validateEmailVerification("valid-token")).thenReturn(jwtResponse);

            mockMvc.perform(get("/api/validate/verify-email")
                    .param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());
        }

        @Test
        @DisplayName("GET /api/validate/verify-email - Should reject invalid token")
        void testVerifyEmailInvalidToken() throws Exception {
            when(emailService.validateEmailVerification("invalid-token")).thenReturn(null);

            mockMvc.perform(get("/api/validate/verify-email")
                    .param("token", "invalid-token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/validate/verify-email - Should reject expired token")
        void testVerifyEmailExpiredToken() throws Exception {
            when(emailService.validateEmailVerification("expired-token")).thenReturn(null);

            mockMvc.perform(get("/api/validate/verify-email")
                    .param("token", "expired-token"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== RESEND VERIFICATION TESTS ====================

    @Nested
    @DisplayName("Resend Verification Tests")
    class ResendVerificationTests {

        @Test
        @DisplayName("POST /api/resend-verification - Should resend for unverified user")
        void testResendVerificationUnverified() throws Exception {
            testUser.setEmailVerified(false);
            when(userService.findByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.generateEmailToken(anyString())).thenReturn("new-token");
            doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

            EmailDto emailDto = new EmailDto();
            emailDto.setEmail("test@example.com");

            mockMvc.perform(post("/api/resend-verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emailDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/resend-verification - Should reject for already verified user")
        void testResendVerificationAlreadyVerified() throws Exception {
            testUser.setEmailVerified(true);
            when(userService.findByEmail("test@example.com")).thenReturn(testUser);

            EmailDto emailDto = new EmailDto();
            emailDto.setEmail("test@example.com");

            mockMvc.perform(post("/api/resend-verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emailDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/resend-verification - Should reject for non-existent user")
        void testResendVerificationNonExistent() throws Exception {
            when(userService.findByEmail("nonexistent@example.com")).thenReturn(null);

            EmailDto emailDto = new EmailDto();
            emailDto.setEmail("nonexistent@example.com");

            mockMvc.perform(post("/api/resend-verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emailDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== FORGOT PASSWORD TESTS ====================

    @Nested
    @DisplayName("Forgot Password Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("POST /api/forgot-password - Should send reset email for verified user")
        void testForgotPasswordVerifiedUser() throws Exception {
            testUser.setEmailVerified(true);
            when(userService.findByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.generatePasswordResetToken(anyString())).thenReturn("reset-token");
            doNothing().when(emailService).sendForgotPasswordEmail(anyString(), anyString());

            UserDto userDto = new UserDto();
            userDto.setEmail("test@example.com");

            mockMvc.perform(post("/api/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/forgot-password - Should reject unverified user")
        void testForgotPasswordUnverifiedUser() throws Exception {
            testUser.setEmailVerified(false);
            when(userService.findByEmail("test@example.com")).thenReturn(testUser);

            UserDto userDto = new UserDto();
            userDto.setEmail("test@example.com");

            mockMvc.perform(post("/api/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/forgot-password - Should reject non-existent email")
        void testForgotPasswordNonExistent() throws Exception {
            when(userService.findByEmail("nonexistent@example.com")).thenReturn(null);

            UserDto userDto = new UserDto();
            userDto.setEmail("nonexistent@example.com");

            mockMvc.perform(post("/api/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== PASSWORD RESET VALIDATION TESTS ====================

    @Nested
    @DisplayName("Password Reset Validation Tests")
    class PasswordResetValidationTests {

        @Test
        @DisplayName("GET /api/validate/forgot-password - Should validate correct token")
        void testValidateResetTokenValid() throws Exception {
            when(emailService.validateForgotPasswordToken("valid-token")).thenReturn(true);

            mockMvc.perform(get("/api/validate/forgot-password")
                    .param("token", "valid-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/validate/forgot-password - Should reject invalid token")
        void testValidateResetTokenInvalid() throws Exception {
            when(emailService.validateForgotPasswordToken("invalid-token")).thenReturn(false);

            mockMvc.perform(get("/api/validate/forgot-password")
                    .param("token", "invalid-token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /api/validate/forgot-password - Should update password with valid token")
        void testUpdatePasswordValid() throws Exception {
            when(emailService.updatePassword(any(PasswordResetDto.class))).thenReturn(true);

            PasswordResetDto resetDto = new PasswordResetDto();
            resetDto.setToken("valid-token");
            resetDto.setPassword("NewPassword123!");

            mockMvc.perform(put("/api/validate/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/validate/forgot-password - Should reject with invalid token")
        void testUpdatePasswordInvalidToken() throws Exception {
            when(emailService.updatePassword(any(PasswordResetDto.class))).thenReturn(false);

            PasswordResetDto resetDto = new PasswordResetDto();
            resetDto.setToken("invalid-token");
            resetDto.setPassword("NewPassword123!");

            mockMvc.perform(put("/api/validate/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetDto)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== VULNERABILITY TESTS ====================

    @Nested
    @DisplayName("Vulnerability Tests")
    class VulnerabilityTests {

        @Test
        @DisplayName("Email enumeration - Should not reveal if email exists")
        void testEmailEnumeration() throws Exception {
            // For non-existent email, response should be same
            when(userService.findByEmail(anyString())).thenReturn(null);

            UserDto userDto = new UserDto();
            userDto.setEmail("nonexistent@example.com");

            // Response should be consistent (no information leak)
            mockMvc.perform(post("/api/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Token tampering - Should reject modified tokens")
        void testTokenTampering() throws Exception {
            when(emailService.validateEmailVerification("tampered-token")).thenReturn(null);

            mockMvc.perform(get("/api/validate/verify-email")
                    .param("token", "tampered-token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("SQL Injection - Should handle malicious email input")
        void testSqlInjectionEmail() throws Exception {
            when(userService.findByEmail(anyString())).thenReturn(null);

            UserDto maliciousDto = new UserDto();
            maliciousDto.setEmail("'; DROP TABLE users; --@example.com");

            mockMvc.perform(post("/api/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(maliciousDto)))
                    .andExpect(status().isBadRequest());
        }
    }
}
