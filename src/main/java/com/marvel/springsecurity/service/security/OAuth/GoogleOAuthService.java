package com.marvel.springsecurity.service.security.OAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.OAuthDto;
import com.marvel.springsecurity.model.OAuthProvider;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.OAuthRepo;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.security.JwtService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.Random;

@Service
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final OAuthTokenExchangeService tokenExchangeService;

    private final OAuthRepo oAuthRepo;

    public GoogleOAuthService(UserRepository userRepository, JwtService jwtService, OAuthTokenExchangeService tokenExchangeService, OAuthRepo oAuthRepo) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenExchangeService = tokenExchangeService;
        this.oAuthRepo = oAuthRepo;
    }

    /**
     * Exchange code for tokens, verify id_token, and create/update user
     * Returns JWT token for authentication
     */
    public JwtResponse exchangeCodeAndFindOrCreate(OAuthDto oAuth) {
        try {
            // Exchange code for id_token and get verified user info
            JsonNode userInfo = tokenExchangeService.exchangeGoogleCodeAndGetUserInfo(oAuth);

            // Populate OAuthDto with user info from id_token
            oAuth.setProviderId(userInfo.get("sub").asText());
            oAuth.setEmail(userInfo.get("email").asText().toLowerCase());
            oAuth.setEmailVerified(userInfo.get("email_verified").asBoolean());
            oAuth.setName(userInfo.has("name") ? userInfo.get("name").asText() : null);
            oAuth.setPicture(userInfo.has("picture") ? userInfo.get("picture").asText() : null);

            // Create or update user and generate JWT
            return findOrCreateAndGenerateToken(oAuth);

        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Failed to authenticate with Google: " + e.getMessage());
        }
    }


    /**
     * Find existing user or create new one, then generate JWT
     * Used by manual code exchange from controller
     */
    public JwtResponse findOrCreateAndGenerateToken(OAuthDto oAuth) {
        if (oAuth.getEmail() == null || oAuth.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException("Email not provided by Google");
        }

        String email = oAuth.getEmail().toLowerCase();
        Optional<OAuthProvider> existingUser = oAuthRepo.findByProviderAndProviderId("GOOGLE", oAuth.getProviderId());
        //for existing user
        if(existingUser.isPresent()){
            Users user = existingUser.get().getUser();
            return generateToken(user);
        }
        //if email already exists with different sign-in method
        if(userRepository.existsByEmail(email)){
            throw new OAuth2AuthenticationException(
                    "ACCOUNT_EXISTS:An account with email '" + email + "' already exists. " +
                    "Please use a different email or login with your existing credentials."
            );
        }
        //for new user
        return register(oAuth);
    }


    private JwtResponse register(OAuthDto oAuth){
        String email = oAuth.getEmail().toLowerCase();
        Users user = new Users();
        user.setUsername(generateUniqueUsername(oAuth.getName(), email));
        user.setName(oAuth.getName());
        user.setEmail(email);
        user.setImageUrl(oAuth.getPicture());
        user.setEmailVerified(oAuth.getEmailVerified());

        OAuthProvider oAuthProvider = new OAuthProvider();
        oAuthProvider.setProvider("GOOGLE");
        oAuthProvider.setProviderId(oAuth.getProviderId());
        user.getOAuthProviders().add(oAuthProvider);

        Users savedUser = userRepository.save(user);
        return generateToken(savedUser);
    }

    private JwtResponse generateToken(Users user){
        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getRoleVersion(), user.getUserId());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        return JwtResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(user.toDto()).build();
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
