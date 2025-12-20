package com.marvel.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OAuthDto {
    // Request fields (from frontend)
    private String provider;      // "GOOGLE" or "GITHUB"
    private String code;          // Authorization code from OAuth provider
    private String redirectUrl;   // Must match configured redirect URI
    private String pendingToken;  // JWT token for retry scenario (contains user info)

    // User info fields (populated after token exchange or from pendingToken)
    private String providerId;
    private String email;
    private Boolean emailVerified;
    private String name;
    private String picture;
}
