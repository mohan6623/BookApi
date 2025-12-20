package com.marvel.springsecurity.config;

import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.RoleVersionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RoleVersionService roleVersionService;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtService jwtService, RoleVersionService roleVersionService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.roleVersionService = roleVersionService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // Extract token from Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                Claims claims = jwtService.extractAllClaims(token);
                username = claims.getSubject();
                String role = claims.get("role", String.class);
                Integer tokenRoleVersion = claims.get("roleVersion", Integer.class);

                // Validate token and set authentication
                if (username != null && role != null && tokenRoleVersion != null
                        && SecurityContextHolder.getContext().getAuthentication() == null
                        && jwtService.validateToken(token)
                        && roleVersionService.isTokenRoleVersionCurrent(username, tokenRoleVersion)) {

                    // Ensure role has ROLE_ prefix
                    if (!role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
                    }

            // Load full UserDetails (UserPrincipal) so downstream code can access userId
            try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
            // Fallback to username-based auth if user details cannot be loaded
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            }
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                // Token expired - log and continue without auth
                logger.debug("JWT token expired: " + e.getMessage());
            } catch (io.jsonwebtoken.security.SignatureException e) {
                // Invalid signature - log and continue without auth
                logger.warn("Invalid JWT signature: " + e.getMessage());
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                // Malformed token - log and continue without auth
                logger.warn("Malformed JWT token: " + e.getMessage());
            } catch (Exception e) {
                // Other errors - log and continue without auth
                logger.error("JWT processing error: " + e.getMessage());
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
