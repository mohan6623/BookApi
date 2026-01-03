package com.marvel.springsecurity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.rateLimiting.RateLimitInfo;
import com.marvel.springsecurity.service.security.rateLimiting.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.*;

/**
 * Interceptor to enforce rate limiting on specified endpoints.
 * Extracts identifier (IP or user email) and checks rate limits before allowing request.
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Value("${whitelist:}")
    private String whiteListIp;

    public RateLimitInterceptor(RateLimiterService rateLimiterService,
                                JwtService jwtService,
                                ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    private Set<String> whitelistedIps;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        // Initialize whitelist lazily (after Spring injection)
        if (whitelistedIps == null) {
            whitelistedIps = new HashSet<>();
            whitelistedIps.add("127.0.0.1");
            if (whiteListIp != null && !whiteListIp.isBlank()) {
                whitelistedIps.addAll(Arrays.asList(whiteListIp.split(",")));
            }
        }

        String ip = getClientIp(request);
        if (whitelistedIps.contains(ip)) {
            return true; // Skip rate limiting for whitelisted IPs
        }

        String endpoint = request.getRequestURI();
        String identifier = extractIdentifier(request, endpoint);

        // Check rate limit
        boolean allowed = rateLimiterService.allowRequest(identifier, endpoint);

        // Get rate limit info for headers
        RateLimitInfo info = rateLimiterService.getLimitInfo(identifier, endpoint);

        // Add rate limit headers to response
        addRateLimitHeaders(response, info);

        if (!allowed) {
            // Rate limit exceeded - return 429
            sendRateLimitExceededResponse(response, info);
            return false;
        }

        return true;
    }

    /**
     * Extract unique identifier for rate limiting.
     * Strategy:
     * - Public endpoints (register, login, available, oauth): Use IP address
     * - Authenticated endpoints (verify, reset, resend): Use email from JWT token
     */
    private String extractIdentifier(HttpServletRequest request, String endpoint) {
        // Endpoints that should use email from JWT (authenticated/semi-authenticated)
        boolean useJwtEmail = endpoint.contains("/verify-email")
                || endpoint.contains("/reset-password")
                || endpoint.contains("/resend-verification");

        if (useJwtEmail) {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                String email = jwtService.extractEmail(token);
                if(email != null){
                    return "email:" + email;
                }else{
                    log.debug("Could not extract email from token, falling back to IP");
                }
            }
        }

        // Default: use IP address
        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    /**
     * Extract JWT token from Authorization header or query parameter.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Check query parameter (for email verification links)
        return request.getParameter("token");
    }


    /**
     * Get client IP address, checking for proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Add rate limit headers to response for API transparency.
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitInfo info) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(info.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, info.remaining())));
        response.setHeader("X-RateLimit-Reset", String.valueOf(info.resetTimeEpochSeconds()));
    }

    /**
     * Send 429 Too Many Requests response with JSON body.
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, RateLimitInfo info)
            throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(info.retryAfterSeconds()));

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", "Rate limit exceeded");
        errorBody.put("message", "Too many requests. Please try again later.");
        errorBody.put("retryAfter", info.retryAfterSeconds());
        errorBody.put("limit", info.limit());
        errorBody.put("endpoint", info.endpoint());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}