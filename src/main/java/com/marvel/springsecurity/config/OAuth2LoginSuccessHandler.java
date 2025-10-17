package com.marvel.springsecurity.config;

import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.RoleVersionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RoleVersionService roleVersionService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from OAuth2 provider
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            // GitHub doesn't always provide email in the default scope
            email = oAuth2User.getAttribute("login") + "@github.oauth";
        }

        // Find or create user
        User user = userRepository.findByUsername(email);
        if (user == null) {
            // Create new user from OAuth2 data
            user = new User();
            user.setUsername(email);
            user.setMail(email);
            user.setRole("USER");
            user.setRoleVersion(0);
            // OAuth2 users don't have a password (or set a random one)
            user.setPassword("OAUTH2_USER");
            userRepository.save(user);
        }

        // Generate JWT token
        String token = jwtService.generateToken(
            user.getUsername(),
            user.getRole(),
            user.getRoleVersion(),
            user.getId()
        );

        // Redirect to frontend with token (adjust URL as needed)
        String redirectUrl = String.format("%s/oauth2/redirect?token=%s", frontendUrl, token);

        response.sendRedirect(redirectUrl);
    }
}
