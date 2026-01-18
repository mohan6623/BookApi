package com.marvel.springsecurity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.rateLimiting.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer(
            RateLimiterService rateLimiterService,
            JwtService jwtService,
            ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(frontendUrl, "https://bookforum.app", "https://www.bookforum.app")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                // Register rate limiting interceptor
                registry.addInterceptor(
                        new RateLimitInterceptor(rateLimiterService, jwtService, objectMapper)).addPathPatterns(
                                "/api/register",
                                "/api/register/**",
                                "/api/login",
                                "/api/validate/**",
                                "/api/update/reset-password",
                                "/api/oauth/callback",
                                "/api/available/**",
                                "/api/forgot-password", // Password reset request - prevents email spam
                                "/api/resend-verification", // Resend verification - prevents email spam
                                "/api/oauth/submit-email", // OAuth email submission - prevents abuse
                                "/api/user", // User updates
                                "/api/user/**" // All user sub-endpoints (profile pic, name, password)
                ).excludePathPatterns(
                        "/api/oauth/health",
                        "/actuator/**");
            }
        };
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register the module that handles Java 8 Date/Time types like Instant
        mapper.registerModule(new JavaTimeModule());

        // Optional: This makes the output a readable string (ISO-8601)
        // instead of a numeric timestamp [1735560000.000000000]
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
