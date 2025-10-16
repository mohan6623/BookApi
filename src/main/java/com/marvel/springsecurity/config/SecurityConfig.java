package com.marvel.springsecurity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    UserDetailsService userDetailsService;
    @Autowired
    JwtFilter jwtFilter;
    @Bean
    public AuthenticationProvider authProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        return provider;
    }
    @Bean
    public SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Enable CORS support
                // Disable CSRF (Cross-Site Request Forgery) protection for the application
                .csrf(customize -> customize.disable())
        // Return 401 without triggering browser basic-auth prompt
//                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
//                    response.setStatus(401);
//        }))
                // Configure authorization for HTTP requests, setting all requests to be authenticated.
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/register","/login","/bookid/").permitAll() // permit auth endpoints
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // Allow OPTIONS for CORS preflight
                        .anyRequest().authenticated() // will permit only after authenticate.
                )
                // Use basic HTTP authentication (username and password in HTTP headers)
                .httpBasic(Customizer.withDefaults())
                .oauth2Login(Customizer.withDefaults())
                // Configure session management to be stateless, meaning no session will be maintained
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Hard Coded values
//    @Bean
//    public UserDetailsService user(){
//
//        UserDetails user = User
//                .withDefaultPasswordEncoder()
//                .username("Mohan")
//                .password("6623")
//                .roles("USER")
//                .build();
//
//        UserDetails user2 = User.withDefaultPasswordEncoder()
//                .username("Marvel")
//                .password("2623")
//                .roles("ADMIN")
//                .build();
//        return new InMemoryUserDetailsManager(user, user2);
//    }

}







//public SecurityFilterChain security(HttpSecurity http) throws Exception {
//    // Disable CSRF (Cross-Site Request Forgery) protection for the application
//    http.csrf(new Customizer<CsrfConfigurer<HttpSecurity>>() {
//        @Override
//        public void customize(CsrfConfigurer<HttpSecurity> csrf) {
//            csrf.disable();
//        }
//    });
//
//    // Configure authorization for HTTP requests, requiring all requests to be authenticated
//    http.authorizeHttpRequests(new Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>() {
//        @Override
//        public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
//            registry.anyRequest().authenticated();
//        }
//    });
//
//    // Use basic HTTP authentication (username and password in HTTP headers)
//    http.httpBasic(Customizer.withDefaults());
//
//    // Configure session management to be stateless, meaning no session will be maintained
//    http.sessionManagement(new Customizer<SessionManagementConfigurer<HttpSecurity>>() {
//        @Override
//        public void customize(SessionManagementConfigurer<HttpSecurity> sessionManagement) {
//            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//        }
//    });
//
//    // Build and return the configured SecurityFilterChain object
//    return http.build();
//}