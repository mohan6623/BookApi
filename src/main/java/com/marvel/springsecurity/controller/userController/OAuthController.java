package com.marvel.springsecurity.controller.userController;

import com.marvel.springsecurity.model.OAuthProvider;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.OAuthRepo;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.security.EmailService;
import com.marvel.springsecurity.service.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/oauth")
@CrossOrigin(origins = "${app.frontend.url:http://localhost:3000}")
public class OAuthController {

    private final UserRepository userRepository;
    private final OAuthRepo oAuthRepo;
    private final JwtService jwtService;
    private final EmailService emailService;

    public OAuthController(UserRepository userRepository, OAuthRepo oAuthRepo, JwtService jwtService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.oAuthRepo = oAuthRepo;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    /**
     * Validate and normalize provider name to prevent XSS
     */
    private String validateProvider(String provider) {
        if (provider == null) {
            return null;
        }
        String normalized = provider.toUpperCase();
        if (normalized.equals("GOOGLE") || normalized.equals("GITHUB")) {
            return normalized;
        }
        return null;
    }

    /**
     * Health check endpoint for OAuth service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OAuth service is running - using Spring Security OAuth2");
    }

    /**
     * Disconnect OAuth provider from user account
     */
    @DeleteMapping("/disconnect/{provider}")
    public ResponseEntity<?> disconnectOAuthProvider(
            @PathVariable String provider,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User must be authenticated"));
        }

        // Validate provider to prevent XSS
        String validatedProvider = validateProvider(provider);
        if (validatedProvider == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid provider. Must be Google or GitHub"));
        }

        Users user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        // Find and remove the OAuth provider
        Optional<OAuthProvider> providerToRemove = user.getOAuthProviders().stream()
                .filter(p -> p.getProvider().equals(validatedProvider))
                .findFirst();

        if (providerToRemove.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "This OAuth provider is not linked to your account",
                    "code", "PROVIDER_NOT_FOUND"));
        }

        // Security check: Ensure user has alternative authentication method
        boolean hasPassword = user.getPassword() != null && !user.getPassword().isEmpty();
        long otherProvidersCount = user.getOAuthProviders().stream()
                .filter(p -> !p.getProvider().equals(validatedProvider))
                .count();

        if (!hasPassword && otherProvidersCount == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "Cannot disconnect your only authentication method. Please set a password first or link another OAuth provider before disconnecting this one.",
                    "code", "LAST_AUTH_METHOD",
                    "suggestion", "Add a password or link another OAuth provider first"));
        }

        OAuthProvider removedProvider = providerToRemove.get();

        // Audit log: Record the OAuth email for security tracking
        String removedOauthEmail = removedProvider.getOauthEmail();
        String removedProviderId = removedProvider.getProviderId();

        user.getOAuthProviders().remove(removedProvider);
        userRepository.save(user);

        // Log disconnect for security audit
        log.info("OAuth disconnect - User: {}, Provider: {}, ProviderId: {}, OAuth Email: {}",
                user.getUsername(), validatedProvider, removedProviderId, removedOauthEmail);

        return ResponseEntity.ok(Map.of(
                "message", "OAuth account successfully disconnected",
                "provider", validatedProvider,
                "code", "SUCCESS",
                "disconnectedEmail", removedOauthEmail != null ? removedOauthEmail : "unknown",
                "remainingAuthMethods", Map.of(
                        "hasPassword", hasPassword,
                        "oauthProviders", user.getOAuthProviders().stream()
                                .map(OAuthProvider::getProvider)
                                .toList())));
    }

    /**
     * Submit email for OAuth registration when email is required
     * Used when OAuth provider (like GitHub) doesn't provide public email
     */
    @PostMapping("/submit-email")
    public ResponseEntity<?> submitOAuthEmail(@RequestBody Map<String, String> request) {
        String pendingToken = request.get("pendingToken");
        String email = request.get("email");
        String usernameFromRequest = request.get("username");
        String nameFromRequest = request.get("name");

        // Validate input
        if (pendingToken == null || pendingToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Pending token is required"));
        }

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Email is required"));
        }

        // Validate email format
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_EMAIL",
                    "message", "Please enter a valid email address"));
        }

        // Validate username if provided
        if (usernameFromRequest != null && !usernameFromRequest.isBlank()) {
            if (usernameFromRequest.length() < 3) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_USERNAME",
                        "message", "Username must be at least 3 characters long"));
            }
            // Check if username is already taken
            if (userRepository.existsByUsername(usernameFromRequest)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "USERNAME_TAKEN",
                        "message", "This username is already taken"));
            }
        }

        // Extract OAuth pending data from token
        Map<String, String> oauthData = jwtService.extractOAuthPendingData(pendingToken);
        if (oauthData == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_TOKEN",
                    "message", "Session expired. Please try logging in again."));
        }

        String providerId = oauthData.get("providerId");
        String nameFromToken = oauthData.get("name");
        String picture = oauthData.get("picture");
        String provider = oauthData.get("provider");

        // Check if email is already taken by a verified account
        Optional<Users> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            Users existingUser = existingUserByEmail.get();

            if (existingUser.isEmailVerified()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "ACCOUNT_EXISTS",
                        "message", "An account with this email already exists. Please login with that account."));
            }

            // Overwrite unverified account (same logic as SpringOAuth2UserService)
            log.info(
                    "Overwriting unverified account during OAuth email submission - Email: {}, Provider: {}, Existing Account Created: {}",
                    email, provider, existingUser.getCreatedAt());
            userRepository.delete(existingUser);
            userRepository.flush();
        }

        // Determine final username and name
        String finalUsername = (usernameFromRequest != null && !usernameFromRequest.isBlank())
                ? usernameFromRequest
                : generateUniqueUsername(nameFromToken, email);
        
        String finalName = (nameFromRequest != null && !nameFromRequest.isBlank())
                ? nameFromRequest
                : (nameFromToken != null ? nameFromToken : email.split("@")[0]);

        // Create new user
        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setName(finalName);
        newUser.setUsername(finalUsername);
        newUser.setImageUrl(picture);
        newUser.setEmailVerified(false); // Need to verify email since it wasn't from OAuth
        newUser.setRole("ROLE_USER");
        newUser.setPassword(""); // No password for OAuth users

        // Create OAuth provider link
        OAuthProvider newProvider = new OAuthProvider();
        newProvider.setProvider(provider);
        newProvider.setProviderId(providerId);
        newProvider.setOauthEmail(email);
        newProvider.setUser(newUser);

        newUser.getOAuthProviders().add(newProvider);

        // Generate email verification token
        String verificationToken = jwtService.generateEmailToken(email);
        newUser.setVerificationToken(verificationToken);

        // Save user
        userRepository.save(newUser);

        // Send verification email
        try {
            emailService.sendVerificationEmail(email, verificationToken);
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
            // Don't fail the request - user is created, they can resend verification later
        }

        log.info("Email submitted for OAuth registration - Email: {}, Provider: {}, ProviderId: {}",
                email, provider, providerId);

        return ResponseEntity.ok(Map.of(
                "message", "Verification email sent. Please check your inbox to complete registration.",
                "email", email));
    }

    /**
     * Complete OAuth registration for new users (Google/GitHub with email)
     * Allows user to choose their username and display name
     */
    @PostMapping("/complete-registration")
    public ResponseEntity<?> completeOAuthRegistration(@RequestBody Map<String, String> request) {
        String pendingToken = request.get("pendingToken");
        String username = request.get("username");
        String name = request.get("name");

        // Validate input
        if (pendingToken == null || pendingToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Pending token is required"));
        }

        if (username == null || username.isBlank() || username.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_USERNAME",
                    "message", "Username must be at least 3 characters long"));
        }

        // Validate username format (only lowercase letters, numbers, underscores)
        if (!username.matches("^[a-z0-9_]+$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_USERNAME",
                    "message", "Username can only contain lowercase letters, numbers, and underscores"));
        }

        // Check if username is already taken
        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "USERNAME_TAKEN",
                    "message", "This username is already taken"));
        }

        // Extract OAuth registration data from token
        Map<String, String> oauthData = jwtService.extractOAuthRegistrationData(pendingToken);
        if (oauthData == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_TOKEN",
                    "message", "Session expired. Please try logging in again."));
        }

        String providerId = oauthData.get("providerId");
        String nameFromToken = oauthData.get("name");
        String picture = oauthData.get("picture");
        String provider = oauthData.get("provider");
        String email = oauthData.get("email");

        // Check if email is already taken by a verified account
        Optional<Users> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            Users existingUser = existingUserByEmail.get();

            if (existingUser.isEmailVerified()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "ACCOUNT_EXISTS",
                        "message", "An account with this email already exists. Please login with that account."));
            }

            // Overwrite unverified account
            log.info("Overwriting unverified account during OAuth registration - Email: {}, Provider: {}",
                    email, provider);
            userRepository.delete(existingUser);
            userRepository.flush();
        }

        // Determine final name
        String finalName = (name != null && !name.isBlank()) ? name : (nameFromToken != null ? nameFromToken : email.split("@")[0]);

        // Create new user
        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setName(finalName);
        newUser.setUsername(username);
        newUser.setImageUrl(picture);
        newUser.setEmailVerified(true); // OAuth with email is trusted
        newUser.setRole("ROLE_USER");
        newUser.setPassword(""); // No password for OAuth users

        // Create OAuth provider link
        OAuthProvider newProvider = new OAuthProvider();
        newProvider.setProvider(provider);
        newProvider.setProviderId(providerId);
        newProvider.setOauthEmail(email);
        newProvider.setUser(newUser);

        newUser.getOAuthProviders().add(newProvider);

        // Save user
        userRepository.save(newUser);

        log.info("OAuth registration completed - Email: {}, Username: {}, Provider: {}, ProviderId: {}",
                email, username, provider, providerId);

        // Generate JWT token for the new user
        String jwtToken = jwtService.generateToken(newUser.getUsername(), newUser.getRole(), 0, newUser.getUserId(), newUser.getImageUrl());

        return ResponseEntity.ok(Map.of(
                "message", "Registration completed successfully",
                "token", jwtToken,
                "userId", newUser.getUserId(),
                "username", newUser.getUsername(),
                "email", newUser.getEmail(),
                "name", newUser.getName(),
                "imageUrl", newUser.getImageUrl() != null ? newUser.getImageUrl() : "",
                "role", newUser.getRole()));
    }

    /**
     * Generate unique username for new OAuth user
     */
    private String generateUniqueUsername(String name, String email) {
        String base = (name != null && !name.isBlank())
                ? name.toLowerCase().replaceAll("[^a-z0-9]", "")
                : email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");

        if (base.length() < 3)
            base = "user" + base;
        String candidate = base;
        int suffix = 1;

        while (userRepository.findByUsername(candidate) != null) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
