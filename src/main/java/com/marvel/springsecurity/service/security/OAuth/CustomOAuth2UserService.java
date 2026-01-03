package com.marvel.springsecurity.service.security.OAuth;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.OAuthDto;
import com.marvel.springsecurity.service.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Manual OAuth 2.0 Service
 * Handles OAuth code exchange from frontend (SPA-friendly approach)
 * Does NOT use Spring's automatic OAuth flow
 */
@Service
public class CustomOAuth2UserService {

    private final GoogleOAuthService googleOAuthService;
    private final GithubOAuthService githubOAuthService;
    private final JwtService jwtService;

    @Autowired
    public CustomOAuth2UserService(GoogleOAuthService googleOAuthService, 
                                   GithubOAuthService githubOAuthService,
                                   JwtService jwtService) {
        this.googleOAuthService = googleOAuthService;
        this.githubOAuthService = githubOAuthService;
        this.jwtService = jwtService;
    }

    /**
     * Manual OAuth Code Exchange
     * Called from /api/oauth/callback endpoint when frontend sends OAuth code
     *
     * Flow:
     * 1. Receives authorization code from frontend
     * 2. Exchanges code for tokens with OAuth provider
     * 3. Verifies ID token (Google) or fetches user info (GitHub)
     * 4. Creates/updates user in database
     * 5. Generates and returns JWT token
     */
    public JwtResponse extractOAuthCode(OAuthDto oAuth) {
        if (oAuth == null || oAuth.getProvider() == null) {
            throw new OAuth2AuthenticationException("Invalid OAuth request");
        }

        if ("GOOGLE".equalsIgnoreCase(oAuth.getProvider())) {
            return googleOAuthService.exchangeCodeAndFindOrCreate(oAuth);
        } else if ("GITHUB".equalsIgnoreCase(oAuth.getProvider())) {
            return githubOAuthService.exchangeCodeAndFindOrCreate(oAuth);
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + oAuth.getProvider());
        }
    }

    /**
     * Complete GitHub OAuth when email is provided by user
     * Called when GitHub doesn't provide email and user submits their email
     */
    public JwtResponse completeGithubOAuthWithEmail(String pendingToken, String email) {
        // Extract and validate pending token
        Map<String, String> pendingData = jwtService.extractOAuthPendingData(pendingToken);
        
        if (pendingData == null) {
            throw new OAuth2AuthenticationException("Invalid or expired pending token. Please try logging in again.");
        }

        // Verify it's a GitHub pending token
        if (!"GITHUB".equals(pendingData.get("provider"))) {
            throw new OAuth2AuthenticationException("Invalid pending token provider");
        }

        // Create OAuthDto with user-provided email
        OAuthDto oAuth = new OAuthDto();
        oAuth.setProvider("GITHUB");
        oAuth.setProviderId(pendingData.get("providerId"));
        oAuth.setName(pendingData.get("name"));
        oAuth.setPicture(pendingData.get("picture"));
        oAuth.setEmail(email);
        oAuth.setEmailVerified(false); // Email provided by user, not verified by GitHub

        // Complete user creation/login
        return githubOAuthService.findOrCreateAndGenerateToken(oAuth);
    }
}
