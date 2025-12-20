package com.marvel.springsecurity.service.security.OAuth;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.OAuthDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Manual OAuth 2.0 Service
 * Handles OAuth code exchange from frontend (SPA-friendly approach)
 * Does NOT use Spring's automatic OAuth flow
 */
@Service
public class CustomOAuth2UserService {

    private final GoogleOAuthService googleOAuthService;
    private final GithubOAuthService githubOAuthService;

    @Autowired
    public CustomOAuth2UserService(GoogleOAuthService googleOAuthService, GithubOAuthService githubOAuthService) {
        this.googleOAuthService = googleOAuthService;
        this.githubOAuthService = githubOAuthService;
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
}
