package com.marvel.springsecurity.config;

import com.marvel.springsecurity.service.security.OAuth.SpringOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final UserDetailsService userDetailsService;
        private final JwtFilter jwtFilter;
        private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
        private final SpringOAuth2UserService springOAuth2UserService;

        public SecurityConfig(UserDetailsService userDetailsService, JwtFilter jwtFilter,
                        OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
                        OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                        SpringOAuth2UserService springOAuth2UserService) {
                this.userDetailsService = userDetailsService;
                this.jwtFilter = jwtFilter;
                this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
                this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
                this.springOAuth2UserService = springOAuth2UserService;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationProvider authProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        public SecurityFilterChain security(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults()) // Enable CORS support
                                // Disable CSRF for stateless API
                                .csrf(customize -> customize.disable())

                                // Return 401 without triggering browser basic-auth prompt
                                .exceptionHandling(
                                                e -> e.authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(401);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                                                }))

                                // Configure authorization for HTTP requests
                                .authorizeHttpRequests(request -> request
                                                // Swagger UI and API docs
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // Health check endpoint
                                                .requestMatchers("/api/health").permitAll()

                                                // Public auth endpoints (with /api prefix)
                                                .requestMatchers("/api/register",
                                                                "/api/login",
                                                                "/api/available/**",
                                                                "/api/forgot-password",
                                                                "/api/resend-verification",
                                                                "/api/user/refresh-token")
                                                .permitAll()

                                                // Manual OAuth callback endpoint
                                                .requestMatchers("/api/oauth/**").permitAll()

                                                // OAuth2 Login Endpoints
                                                .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()

                                                // Allow OPTIONS for CORS preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // Public read endpoints (browsing without login)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/books",
                                                                "/api/bookid/**",
                                                                "/api/books/search",
                                                                "/api/book/*/ratings",
                                                                "/api/book/*/comment",
                                                                "/api/book/categories",
                                                                "/api/book/authors")
                                                .permitAll()
                                                // email validation(reset password and email verification)
                                                .requestMatchers("/api/validate/**").permitAll()

                                                // Everything else requires authentication
                                                .anyRequest().authenticated())

                                // Stateless session for REST API (no sessions, only JWT)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // OAuth2 Login Configuration
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(springOAuth2UserService))
                                                .authorizationEndpoint(authorization -> authorization
                                                                .baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/login/oauth2/code/*"))
                                                .successHandler(oAuth2LoginSuccessHandler)
                                                .failureHandler(oAuth2LoginFailureHandler))

                                // Register authentication provider
                                .authenticationProvider(authProvider())

                                // Add JWT filter before UsernamePasswordAuthenticationFilter
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}
