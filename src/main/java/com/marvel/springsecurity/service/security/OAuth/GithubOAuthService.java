package com.marvel.springsecurity.service.security.OAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.OAuthDto;
import com.marvel.springsecurity.model.OAuthProvider;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.OAuthRepo;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class GithubOAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OAuthTokenExchangeService tokenExchangeService;

    @Autowired
    private OAuthRepo oAuthRepo;

    /**
     * Exchange code for access_token, fetch user info, and create/update user
     * OR if frontend provides pendingToken (retry scenario), decode and use that data
     */
    public JwtResponse exchangeCodeAndFindOrCreate(OAuthDto oAuth) {
        try {
            // Check if frontend provided pendingToken (retry scenario)
            if (oAuth.getPendingToken() != null && !oAuth.getPendingToken().isBlank()) {
                // Decode and validate pending token
                Map<String, String> pendingData = jwtService.extractOAuthPendingData(oAuth.getPendingToken());
                
                if (pendingData == null) {
                    throw new OAuth2AuthenticationException("Invalid or expired pending token");
                }
                
                // Ensure email is provided
                if (oAuth.getEmail() == null || oAuth.getEmail().isBlank()) {
                    throw new OAuth2AuthenticationException("Email is required");
                }
                
                // Populate OAuthDto from verified token data
                oAuth.setProviderId(pendingData.get("providerId"));
                oAuth.setName(pendingData.get("name"));
                oAuth.setPicture(pendingData.get("picture"));
                oAuth.setProvider(pendingData.get("provider"));
                oAuth.setEmailVerified(false); // User-provided email is not verified
                
                return findOrCreateAndGenerateToken(oAuth);
            }

            // First attempt - need to fetch user info from GitHub
            JsonNode userInfo = tokenExchangeService.exchangeGithubCodeAndGetUserInfo(
                    oAuth.getCode(),
                    oAuth.getRedirectUrl()
            );

            oAuth.setProviderId(userInfo.get("id").asText());

            String email = userInfo.has("email") && !userInfo.get("email").isNull()
                    ? userInfo.get("email").asText()
                    : null;

            // If frontend provided email, use it
            if (email == null || email.isBlank()) {
                email = oAuth.getEmail();
            }

            // If still no email, generate pending token and throw exception
            if (email == null || email.isBlank()) {
                String name = userInfo.has("name") && !userInfo.get("name").isNull()
                        ? userInfo.get("name").asText()
                        : userInfo.get("login").asText();

                String picture = userInfo.has("avatar_url") ? userInfo.get("avatar_url").asText() : null;
                String providerId = userInfo.get("id").asText();

                // Generate secure JWT token containing user info
                String pendingToken = jwtService.generateOAuthPendingToken(
                        providerId, 
                        name, 
                        picture, 
                        "GITHUB"
                );

                // Throw exception with token reference
                throw new OAuth2AuthenticationException("EMAIL_REQUIRED:" + pendingToken);
            }

            oAuth.setEmail(email.toLowerCase());

            // Email is verified only if it came from GitHub (not user input)
            boolean emailFromGithub = userInfo.has("email") && !userInfo.get("email").isNull()
                    && email.equalsIgnoreCase(userInfo.get("email").asText());
            oAuth.setEmailVerified(emailFromGithub);

            String name = userInfo.has("name") && !userInfo.get("name").isNull()
                    ? userInfo.get("name").asText()
                    : userInfo.get("login").asText();
            oAuth.setName(name);

            oAuth.setPicture(userInfo.has("avatar_url") ? userInfo.get("avatar_url").asText() : null);

            return findOrCreateAndGenerateToken(oAuth);

        } catch (OAuth2AuthenticationException e) {
            throw e; // Re-throw OAuth exceptions
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Failed to authenticate with GitHub: " + e.getMessage());
        }
    }


    /**
     * Find existing user or create new one and return User1 object
     * SECURITY: Checks providerId first to allow returning users
     * REJECTS email conflicts to prevent account takeover (no auto-linking)
     */
    public Users findOrCreate(OAuthDto oAuth) {
        if (oAuth.getEmail() == null || oAuth.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException("Email not provided by GitHub. Please ensure email is public in GitHub settings.");
        }

        String email = oAuth.getEmail().toLowerCase();
        String providerId = oAuth.getProviderId();

        // STEP 1: Check if this specific GitHub account is already linked
        Optional<OAuthProvider> existingOAuthProvider = oAuthRepo.findByProviderAndProviderId("GITHUB", providerId);

        if (existingOAuthProvider.isPresent()) {
            // User has already linked this GitHub account - ALLOW LOGIN ✅
            Users user = existingOAuthProvider.get().getUser();

            // Update profile info
            user.setEmailVerified(true);
            if (oAuth.getPicture() != null && !oAuth.getPicture().isBlank()) {
                user.setImageUrl(oAuth.getPicture());
            }

            return userRepository.save(user);
        }

        // STEP 2: Check if email is already taken by another user
        Optional<Users> existingUserByEmail = userRepository.findByEmail(email);

        if (existingUserByEmail.isPresent()) {
            // Email already exists - REJECT to prevent account takeover ✅
            // DO NOT auto-link - this prevents attackers from hijacking accounts
            throw new OAuth2AuthenticationException(
                "ACCOUNT_EXISTS:An account with email '" + email + "' already exists. " +
                "Please use a different email or login with your existing credentials."
            );
        }

        // STEP 3: Email is available - create new account ✅
        Users user = new Users();
        user.setEmail(email);
        user.setName(oAuth.getName());
        user.setEmailVerified(oAuth.getEmailVerified() != null ? oAuth.getEmailVerified() : false);
        user.setRole("ROLE_USER");
        user.setUsername(generateUniqueUsername(oAuth.getName(), email));
        user.setImageUrl(oAuth.getPicture());

        // Create OAuth provider
        OAuthProvider oAuthProvider = new OAuthProvider();
        oAuthProvider.setProvider("GITHUB");
        oAuthProvider.setProviderId(oAuth.getProviderId());
        oAuthProvider.setUser(user);
        user.getOAuthProviders().add(oAuthProvider);

        return userRepository.save(user);
    }

    /**
     * Find existing user or create new one, then generate JWT
     * Used by manual code exchange from controller
     */
    public JwtResponse findOrCreateAndGenerateToken(OAuthDto oAuth) {
        Users user = findOrCreate(oAuth);

        // Generate JWT token
        String token = jwtService.generateToken(user.getUsername(), user.getRole());

        return JwtResponse.builder()
                .token(token)
                .user(user.toDto())
                .build();
    }

    private String generateUniqueUsername(String baseName, String emailFallback) {
        String base;
        if (baseName != null && !baseName.isBlank()) {
            base = normalizeUsername(baseName);
        } else if (emailFallback != null) {
            base = normalizeUsername(emailFallback.replaceAll("@.*", ""));
        } else {
            base = "user";
        }

        String candidate = base;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + new Random().nextInt(10000);
        }
        return candidate;
    }

    private String normalizeUsername(String input) {
        String normalized = input.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
        if (normalized.isBlank()) {
            return "user";
        }
        return normalized;
    }
}
