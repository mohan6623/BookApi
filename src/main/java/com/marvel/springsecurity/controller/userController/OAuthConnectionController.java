package com.marvel.springsecurity.controller.userController;

import com.marvel.springsecurity.exception.ForbiddenException;
import com.marvel.springsecurity.model.OAuthProvider;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.OAuthRepo;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GitHub-style OAuth Connection Controller
 * 
 * This controller handles OAuth connection flow SEPARATELY from the login flow.
 * Key difference: Uses dedicated callback endpoints for connections, not the
 * login endpoints.
 * 
 * Flow:
 * 1. User clicks "Connect Google/GitHub" on profile page
 * 2. Frontend calls GET /api/oauth/connect/{provider} with JWT
 * 3. Backend generates state token, stores username in state, returns auth URL
 * 4. User is redirected to OAuth provider
 * 5. OAuth provider redirects to /api/oauth/connect/callback/{provider}
 * 6. Backend exchanges code for token, gets user info, links account
 * 7. Redirects to frontend success page
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth/connect")
@CrossOrigin(origins = "${app.frontend.url:http://localhost:3000}", allowCredentials = "true")
public class OAuthConnectionController {

    private final UserRepository userRepository;
    private final OAuthRepo oAuthRepo;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();

    // In-memory state storage (for simplicity - use Redis in production for scaling)
    private final Map<String, ConnectionState> pendingConnections = new HashMap<>();

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    // Google OAuth
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    // GitHub OAuth for Connect (separate credentials)
    @Value("${github.connect.client-id}")
    private String githubClientId;

    @Value("${github.connect.client-secret}")
    private String githubClientSecret;

    public OAuthConnectionController(UserRepository userRepository, OAuthRepo oAuthRepo, JwtService jwtService) {
        this.userRepository = userRepository;
        this.oAuthRepo = oAuthRepo;
        this.jwtService = jwtService;
    }

    /**
     * Simple state object to track pending connections
     */
    private static class ConnectionState {
        String username;
        String provider;
        long createdAt;

        ConnectionState(String username, String provider) {
            this.username = username;
            this.provider = provider;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 5 * 60 * 1000; // 5 minutes
        }
    }

    /**
     * Validate provider name
     */
    private String validateProvider(String provider) {
        if (provider == null)
            return null;
        String normalized = provider.toUpperCase();
        if (normalized.equals("GOOGLE") || normalized.equals("GITHUB")) {
            return normalized;
        }
        return null;
    }

    /**
     * STEP 1: Initiate OAuth connection
     * User must be authenticated (JWT). This endpoint returns the OAuth
     * authorization URL.
     */
    @GetMapping("/{provider}")
    public ResponseEntity<?> initiateConnection(
            @PathVariable String provider,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "You must be logged in to connect an OAuth provider",
                    "code", "UNAUTHORIZED"));
        }

        String validatedProvider = validateProvider(provider);
        if (validatedProvider == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid provider. Must be 'google' or 'github'",
                    "code", "INVALID_PROVIDER"));
        }

        // Check if user already has this provider linked
        Users user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new ForbiddenException("User Not Found"));
        if (user != null) {
            boolean alreadyLinked = user.getOAuthProviders().stream()
                    .anyMatch(p -> p.getProvider().equals(validatedProvider));
            if (alreadyLinked) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error",
                        "You already have " + validatedProvider
                                + " linked. Disconnect it first to link a different account.",
                        "code", "ALREADY_LINKED"));
            }
        }

        // Generate state token with username embedded
        String state = UUID.randomUUID().toString();

        // Store pending connection (username + provider)
        pendingConnections.put(state, new ConnectionState(userDetails.getUsername(), validatedProvider));

        // Clean up old expired states
        pendingConnections.entrySet().removeIf(e -> e.getValue().isExpired());

        // Build OAuth authorization URL with OUR callback URL (not Spring Security's)
        String callbackUrl = backendUrl + "/api/oauth/connect/callback/" + provider.toLowerCase();
        String authUrl = buildAuthorizationUrl(validatedProvider, state, callbackUrl);

        return ResponseEntity.ok(Map.of(
                "authUrl", authUrl,
                "state", state,
                "message", "Redirect to this URL to connect " + validatedProvider));
    }

    /**
     * STEP 2: OAuth callback for connections
     * This is called by the OAuth provider after user grants permission.
     * This is a SEPARATE endpoint from Spring Security's login callback.
     */
    @GetMapping("/callback/{provider}")
    public void handleCallback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {
        String validatedProvider = validateProvider(provider);

        // Handle OAuth errors
        if (error != null) {
            response.sendRedirect(frontendUrl + "/profile?error=" +
                    URLEncoder.encode("OAuth authorization was denied: " + error, StandardCharsets.UTF_8));
            return;
        }

        if (code == null || state == null) {
            response.sendRedirect(frontendUrl + "/profile?error=" +
                    URLEncoder.encode("Missing authorization code or state", StandardCharsets.UTF_8));
            return;
        }

        // Validate state and get pending connection
        ConnectionState connectionState = pendingConnections.remove(state);
        if (connectionState == null || connectionState.isExpired()) {
            response.sendRedirect(frontendUrl + "/profile?error=" +
                    URLEncoder.encode("Invalid or expired connection state. Please try again.",
                            StandardCharsets.UTF_8));
            return;
        }

        if (!connectionState.provider.equals(validatedProvider)) {
            response.sendRedirect(frontendUrl + "/profile?error=" +
                    URLEncoder.encode("Provider mismatch. Please try again.", StandardCharsets.UTF_8));
            return;
        }

        try {
            // Exchange code for access token
            String callbackUrl = backendUrl + "/api/oauth/connect/callback/" + provider.toLowerCase();
            Map<String, Object> tokenResponse = exchangeCodeForToken(validatedProvider, code, callbackUrl);
            String accessToken = (String) tokenResponse.get("access_token");

            if (accessToken == null) {
                throw new RuntimeException("Failed to get access token");
            }

            // Get user info from OAuth provider
            Map<String, Object> userInfo = getUserInfo(validatedProvider, accessToken);
            String providerId = getProviderId(validatedProvider, userInfo);
            String oauthEmail = (String) userInfo.get("email");

            // Find the user who initiated the connection
            Users user = userRepository.findByUsername(connectionState.username)
                    .orElse(null);
            if (user == null) {
                response.sendRedirect(frontendUrl + "/profile?error=" +
                        URLEncoder.encode("User not found. Please login again.", StandardCharsets.UTF_8));
                return;
            }

            // Check if this OAuth account is already linked to another user
            Optional<OAuthProvider> existingLink = oAuthRepo.findByProviderAndProviderId(validatedProvider, providerId);
            if (existingLink.isPresent()) {
                if (!existingLink.get().getUser().getUserId().equals(user.getUserId())) {
                    response.sendRedirect(frontendUrl + "/profile?error=" +
                            URLEncoder.encode(
                                    "This " + validatedProvider + " account is already linked to another user.",
                                    StandardCharsets.UTF_8));
                    return;
                }
                // Already linked to this user - success (idempotent)
                response.sendRedirect(frontendUrl + "/profile?connected=" + validatedProvider);
                return;
            }

            // Link the OAuth account
            OAuthProvider newProvider = new OAuthProvider();
            newProvider.setProvider(validatedProvider);
            newProvider.setProviderId(providerId);
            newProvider.setOauthEmail(oauthEmail);
            newProvider.setUser(user);

            user.getOAuthProviders().add(newProvider);
            userRepository.save(user);

            log.info("OAuth connected - User: {}, Provider: {}, ProviderId: {}",
                    user.getUsername(), validatedProvider, providerId);

            // Redirect to frontend success
            response.sendRedirect(frontendUrl + "/profile?connected=" + validatedProvider);

        } catch (Exception e) {
            log.error("OAuth connection error: {}", e.getMessage(), e);
            response.sendRedirect(frontendUrl + "/profile?error=" +
                    URLEncoder.encode("Failed to connect OAuth: " + e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Build OAuth authorization URL
     */
    private String buildAuthorizationUrl(String provider, String state, String callbackUrl) {
        if (provider.equals("GOOGLE")) {
            return "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8) +
                    "&response_type=code" +
                    "&scope=" + URLEncoder.encode("openid profile email", StandardCharsets.UTF_8) +
                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                    "&access_type=offline" +
                    "&prompt=consent";
        } else if (provider.equals("GITHUB")) {
            return "https://github.com/login/oauth/authorize?" +
                    "client_id=" + URLEncoder.encode(githubClientId, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode("user:email", StandardCharsets.UTF_8) +
                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }

    /**
     * Exchange authorization code for access token
     */
    private Map<String, Object> exchangeCodeForToken(String provider, String code, String callbackUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("redirect_uri", callbackUrl);
        params.add("grant_type", "authorization_code");

        String tokenUrl;
        if (provider.equals("GOOGLE")) {
            tokenUrl = "https://oauth2.googleapis.com/token";
            params.add("client_id", googleClientId);
            params.add("client_secret", googleClientSecret);
        } else if (provider.equals("GITHUB")) {
            tokenUrl = "https://github.com/login/oauth/access_token";
            params.add("client_id", githubClientId);
            params.add("client_secret", githubClientSecret);
        } else {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });

        return response.getBody();
    }

    /**
     * Get user info from OAuth provider
     */
    private Map<String, Object> getUserInfo(String provider, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);
        String userInfoUrl;

        if (provider.equals("GOOGLE")) {
            userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
        } else if (provider.equals("GITHUB")) {
            userInfoUrl = "https://api.github.com/user";
        } else {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });
        return response.getBody();
    }

    /**
     * Extract provider-specific user ID
     */
    private String getProviderId(String provider, Map<String, Object> userInfo) {
        if (provider.equals("GOOGLE")) {
            return (String) userInfo.get("sub");
        } else if (provider.equals("GITHUB")) {
            Object id = userInfo.get("id");
            return id != null ? id.toString() : null;
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
}
