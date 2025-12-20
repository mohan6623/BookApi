package com.marvel.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthErrorResponse {
    private String error;
    private String message;
    private String pendingToken; // JWT token containing user info (signed and secure)
}

