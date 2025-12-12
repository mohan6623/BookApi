package com.marvel.springsecurity.config;

import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported authentication principal");
            return;
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        Integer userId = (Integer) attributes.get("app_user_id");
        String username = (String) attributes.get("app_username");
        String role = (String) attributes.get("app_role");

        System.out.println("-----------------------------------------------------------");
        System.out.println(username);
        System.out.println(userId);
        System.out.println(role);
        System.out.println("-----------------------------------------------------------");


        // Fallback: If custom username is missing, try standard attributes (email or name) for DB lookup
        if (username == null) {
            username = (String) attributes.get("email");
            System.out.println("-----------------------------------------------------------");
            System.out.println(username);
            if (username == null) {
                username = (String) attributes.get("name");
            }
        }

        if (userId == null && username != null) {
            User user = userRepository.findByUsername(username);
            if (user != null) {
                userId = user.getId();
                if (role == null) role = user.getRole();
            }
        }

        if (username == null) {
            username = String.valueOf(oAuth2User.getAttributes().getOrDefault("name", "user"));
        }

        if (role == null) {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
            role = roles.stream().findFirst().orElse("ROLE_USER");
        }

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User ID not available after OAuth2 login");
            return;
        }

        // TODO: inject RoleVersionService if you want real roleVersion; using 0 as baseline
        String token = jwtService.generateToken(username, role, 0, userId);

        String redirectUrl = frontendUrl + "/oauth2/success?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
