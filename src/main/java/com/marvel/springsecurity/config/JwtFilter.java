package com.marvel.springsecurity.config;

import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.RoleVersionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    JwtService jwtService;

    @Autowired
    RoleVersionService roleVersionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")){
            token = authHeader.substring(7);
            try {
                Claims claims = jwtService.extractAllClaims(token);
                username = claims.getSubject();
                String role = claims.get("role", String.class);
                Integer tokenRoleVersion = claims.get("roleVersion", Integer.class);

                if (username != null && role != null && tokenRoleVersion != null
                        && SecurityContextHolder.getContext().getAuthentication() == null
                        && jwtService.validateToken(token)
                        && roleVersionService.isTokenRoleVersionCurrent(username, tokenRoleVersion)) {

                    if (!role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
                    }
                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ex) {
                // Invalid token; proceed without setting authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}
