package com.marvel.springsecurity.controller.userController;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.OAuthDto;
import com.marvel.springsecurity.dto.OAuthErrorResponse;
import com.marvel.springsecurity.service.security.OAuth.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth")
@CrossOrigin(origins = "${app.frontend.url:http://localhost:3000}")
public class OAuthController {

    @Autowired
    private CustomOAuth2UserService oAuth2UserService;

    /**
     * Endpoint to handle OAuth code from frontend
     * Frontend sends: { provider: "GOOGLE", code: "...", redirectUrl: "..." }
     * Backend returns: JWT token
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleOAuthCallback(@RequestBody OAuthDto oAuthDto) {
        try {
            if (oAuthDto.getCode() == null || oAuthDto.getCode().isBlank()) {
                return ResponseEntity.badRequest().body("Authorization code is required");
            }

            if (oAuthDto.getProvider() == null || oAuthDto.getProvider().isBlank()) {
                return ResponseEntity.badRequest().body("Provider is required (GOOGLE or GITHUB)");
            }

            if (oAuthDto.getRedirectUrl() == null || oAuthDto.getRedirectUrl().isBlank()) {
                return ResponseEntity.badRequest().body("Redirect URL is required");
            }

            // Exchange code for tokens and create/update user
            JwtResponse response = oAuth2UserService.extractOAuthCode(oAuthDto);

            return ResponseEntity.ok(response);

        } catch (OAuth2AuthenticationException e) {
            String message = e.getMessage();
            
            // Handle EMAIL_REQUIRED case with JWT pending token
            if (message != null && message.startsWith("EMAIL_REQUIRED:")) {
                String pendingToken = message.substring("EMAIL_REQUIRED:".length());
                
                OAuthErrorResponse errorResponse = new OAuthErrorResponse();
                errorResponse.setError("EMAIL_REQUIRED");
                errorResponse.setMessage("GitHub account does not have a public email. Please provide your email address.");
                errorResponse.setPendingToken(pendingToken);
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Handle ACCOUNT_EXISTS case (email conflict)
            if (message != null && message.startsWith("ACCOUNT_EXISTS:")) {
                String errorMessage = message.substring("ACCOUNT_EXISTS:".length());

                OAuthErrorResponse errorResponse = new OAuthErrorResponse();
                errorResponse.setError("ACCOUNT_EXISTS");
                errorResponse.setMessage(errorMessage);
                errorResponse.setPendingToken(null);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // Generic OAuth error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("OAuth authentication failed: " + message);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during OAuth authentication: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for OAuth service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OAuth service is running");
    }
}
