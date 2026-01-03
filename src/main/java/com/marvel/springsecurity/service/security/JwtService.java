package com.marvel.springsecurity.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:1800000}") // 30 minutes default
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private long refreshExpiration;

    // 24 hours for email verification tokens
    private static final long EMAIL_VERIFICATION_EXPIRATION = 86400000;

    public String generateEmailToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "email_verification");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EMAIL_VERIFICATION_EXPIRATION))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generatePasswordResetToken(String email){
        Map<String, Object> claim = new HashMap<>();
        claim.put("type", "password_reset");
        return Jwts.builder()
                .setClaims(claim)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }



    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256).compact();
    }

    public String generateToken(String username, String role, int roleVersion, int userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("roleVersion", roleVersion);
        claims.put("userId", userId);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256).compact();
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256).compact();
    }

    private Key getKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build().parseClaimsJws(token).getBody();
    }

    public String extractEmail(String token){
        try{
            if(isTokenExpired(token)) return null;
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        }catch(Exception e){
            return null; // invalid token
        }

    }

    public String extractEmailFromResetToken(String token){
        try {
            Claims claims = extractAllClaims(token);
            if(!"password_reset".equals(claims.get("type"))){
                return null;
            }
            return claims.getSubject();
        } catch (Exception e) {
            return null; // Token expired or invalid
        }
    }

    // Lightweight validation for stateless auth
    public boolean validateToken(String token) {
        return !isTokenExpired(token);
    }


    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername()) && isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generate temporary token for OAuth pending state (when email is required)
     * Token expires in 30 minutes
     */
    public String generateOAuthPendingToken(String providerId, String name, String picture, String provider) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "oauth_pending");
        claims.put("providerId", providerId);
        claims.put("name", name);
        claims.put("picture", picture);
        claims.put("provider", provider);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("oauth_pending")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1800000)) // 30 minutes
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract OAuth pending data from temporary token
     * Returns null if token is invalid or expired
     */
    public Map<String, String> extractOAuthPendingData(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            // Verify token type
            if (!"oauth_pending".equals(claims.get("type"))) {
                return null;
            }
            
            Map<String, String> data = new HashMap<>();
            data.put("providerId", (String) claims.get("providerId"));
            data.put("name", (String) claims.get("name"));
            data.put("picture", (String) claims.get("picture"));
            data.put("provider", (String) claims.get("provider"));
            
            return data;
        } catch (Exception e) {
            return null;
        }
    }
}
