package com.marvel.springsecurity.service.security.OAuth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvel.springsecurity.dto.OAuthDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Service
public class OAuthTokenExchangeService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String githubClientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String githubClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Exchange Google authorization code for tokens and decode id_token
     * Returns user info from the verified id_token
     */
    public JsonNode exchangeGoogleCodeAndGetUserInfo(OAuthDto oAuthDto) throws Exception {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", oAuthDto.getCode());
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", oAuthDto.getRedirectUrl());
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);
            JsonNode tokenResponse = objectMapper.readTree(response.getBody());

            // Get id_token from response (contains user info)
            if (tokenResponse.has("id_token")) {
                String idToken = tokenResponse.get("id_token").asText();
                // Verify and decode id_token
                return verifyAndDecodeGoogleIdToken(idToken);
            } else {
                throw new RuntimeException("No id_token in Google token response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange Google code: " + e.getMessage(), e);
        }
    }

    /**
     * Verify and decode Google ID Token (JWT)
     */
    private JsonNode verifyAndDecodeGoogleIdToken(String idToken) throws Exception {
        DecodedJWT jwt = JWT.decode(idToken);

        // Get Google's public key to verify signature
        JwkProvider provider = new UrlJwkProvider(new URI("https://www.googleapis.com/oauth2/v3/certs").toURL());
        Jwk jwk = provider.get(jwt.getKeyId());
        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

        // Verify signature
        algorithm.verify(jwt);

        // Verify issuer
        String issuer = jwt.getIssuer();
        if (!issuer.equals("https://accounts.google.com") && !issuer.equals("accounts.google.com")) {
            throw new SecurityException("Invalid issuer: " + issuer);
        }

        // Verify audience (client_id)
        if (!jwt.getAudience().contains(googleClientId)) {
            throw new SecurityException("Invalid audience");
        }

        // Check expiration
        if (jwt.getExpiresAt().getTime() < System.currentTimeMillis()) {
            throw new SecurityException("Token expired");
        }

        // Extract and return payload
        String payload = new String(Base64.getUrlDecoder().decode(jwt.getPayload()));
        return objectMapper.readTree(payload);
    }

    /**
     * Exchange GitHub authorization code for access_token
     * Then fetch user info from GitHub API
     * GitHub doesn't provide id_token, so we need to call user API
     */
    public JsonNode exchangeGithubCodeAndGetUserInfo(String code, String redirectUri) throws Exception {
        String tokenUrl = "https://github.com/login/oauth/access_token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", githubClientId);
        params.add("client_secret", githubClientSecret);
        params.add("redirect_uri", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);
            JsonNode tokenResponse = objectMapper.readTree(response.getBody());

            if (tokenResponse.has("error")) {
                throw new RuntimeException("GitHub OAuth error: " + tokenResponse.get("error").asText());
            }

            // Get access_token
            String accessToken = tokenResponse.get("access_token").asText();

            // Fetch user info using access_token
            return getGithubUserInfo(accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange GitHub code: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch GitHub user info using access token
     */
    private JsonNode getGithubUserInfo(String accessToken) throws Exception {
        String userInfoUrl = "https://api.github.com/user";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, String.class);

        return objectMapper.readTree(response.getBody());
    }
}

