package com.marvel.springsecurity.config;

import com.marvel.springsecurity.service.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2 Login Success Handler for LOGIN/REGISTRATION flow only.
 * 
 * Note: OAuth CONNECTION flow (linking OAuth to existing accounts) is handled
 * by OAuthConnectionController with completely separate endpoints.
 * This handler ONLY handles login/registration via OAuth.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported authentication principal");
            return;
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Check if email was missing (Scenario B: GitHub without public email)
        if (attributes.containsKey("email_missing") && (Boolean) attributes.get("email_missing")) {
            String providerId = (String) attributes.get("provider_id");
            String name = (String) attributes.get("name");
            String picture = (String) attributes.get("picture");
            String provider = (String) attributes.getOrDefault("provider", "GITHUB");

            String pendingToken = jwtService.generateOAuthPendingToken(providerId, name, picture, provider);

            String redirectUrl = frontendUrl + "/oauth2/email-required?token=" +
                    URLEncoder.encode(pendingToken, StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
            return;
        }

        // Check if this is a new registration (user needs to choose username/display
        // name)
        if (attributes.containsKey("new_registration") && (Boolean) attributes.get("new_registration")) {
            String providerId = (String) attributes.get("provider_id");
            String name = (String) attributes.get("name");
            String picture = (String) attributes.get("picture");
            String provider = (String) attributes.getOrDefault("provider", "GOOGLE");
            String email = (String) attributes.get("oauth_email");
            String suggestedUsername = (String) attributes.get("suggested_username");

            String pendingToken = jwtService.generateOAuthRegistrationToken(providerId, name, picture, provider, email,
                    suggestedUsername);

            String redirectUrl = frontendUrl + "/oauth2/complete-registration?token=" +
                    URLEncoder.encode(pendingToken, StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
            return;
        }

        Integer userId = (Integer) attributes.get("app_user_id");
        String username = (String) attributes.get("app_username");
        String role = (String) attributes.get("app_role");
        String imageUrl = (String) attributes.get("app_image_url");

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "User ID not available after OAuth2 login");
            return;
        }

        // Generate JWT token for the user
        String token = jwtService.generateToken(username, role, 0, userId, imageUrl);

        String redirectUrl = frontendUrl + "/oauth2/success?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
