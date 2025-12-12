package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Autowired
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String oAuth2Provider = userRequest.getClientRegistration().getRegistrationId(); // google or github
        Map<String, Object> attributes = oAuth2User.getAttributes();

        System.out.println("OAuth2 Attributes for " + oAuth2Provider + ": " + attributes);

        if ("google".equalsIgnoreCase(oAuth2Provider)) {
            return handleGoogleUser(attributes);
        } else if ("github".equalsIgnoreCase(oAuth2Provider)) {
            return handleGithubUser(attributes);
        } else {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + oAuth2Provider);
        }
    }

    private OAuth2User handleGoogleUser(Map<String, Object> attributes) {
        String provider = "GOOGLE";
        String providerId = Objects.toString(attributes.get("sub"), null);
        String email = toLower(Objects.toString(attributes.get("email"), null));
        boolean emailVerified = Boolean.TRUE.equals(attributes.get("email_verified"));
        String name = Objects.toString(attributes.get("name"), null);
        String picture = Objects.toString(attributes.get("picture"), null);

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not provided by Google");
        }

        User user = findOrCreateUser(provider, providerId, email, emailVerified, name, picture);

        Map<String, Object> principalAttrs = new HashMap<>(attributes);
        principalAttrs.put("app_user_id", user.getId());
        principalAttrs.put("app_username", user.getUsername());
        principalAttrs.put("app_role", user.getRole());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
                principalAttrs,
                "sub"
        );
    }

    private OAuth2User handleGithubUser(Map<String, Object> attributes) {
        String provider = "GITHUB";
        String providerId = Objects.toString(attributes.get("id"), null);
        String login = Objects.toString(attributes.get("login"), null);
        String name = Objects.toString(attributes.get("name"), null);
        String avatarUrl = Objects.toString(attributes.get("avatar_url"), null);
        String email = toLower(Objects.toString(attributes.get("email"), null));

        // Email is required in your chosen policy (Option A). If GitHub does not send it here,
        // the application should be configured to fetch it via the /user/emails API using user:email scope.
        // For simplicity here, we fail fast if email is missing.
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not provided by GitHub");
        }

        boolean emailVerified = true; // GitHub's primary email is typically verified if returned.

        // Prefer full name, fallback to login
        String displayName = (name != null && !name.isBlank()) ? name : login;

        User user = findOrCreateUser(provider, providerId, email, emailVerified, displayName, avatarUrl);

        Map<String, Object> principalAttrs = new HashMap<>(attributes);
        principalAttrs.put("app_user_id", user.getId());
        principalAttrs.put("app_username", user.getUsername());
        principalAttrs.put("app_role", user.getRole());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
                principalAttrs,
                "id"
        );
    }

    private User findOrCreateUser(String provider, String providerId, String email, boolean emailVerified,
                                  String name, String avatarUrl) {

        // First try to find by email to avoid duplicate accounts across providers
        Optional<User> existingByEmail = userRepository.findByMail(email);
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
//            updateFromOAuth(user, provider, providerId, emailVerified, name, avatarUrl);
            return userRepository.save(user);

//            verify that how every application hanlding single account with myltiple OAuth provider
        }

        // Then try by provider + providerId
        Optional<User> existingByProvider = userRepository.findByProviderAndProviderId(provider, providerId);
        if (existingByProvider.isPresent()) {
            User user = existingByProvider.get();
            updateFromOAuth(user, provider, providerId, emailVerified, name, avatarUrl);
            return userRepository.save(user);
        }

        // Create a new user
        User user = new User();
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setMail(email);
        user.setEmailVerified(emailVerified);
        user.setRole("ROLE_USER");
        user.setUsername(generateUniqueUsername(name != null ? name : email, email));
        user.setImageUrl(avatarUrl);
        return userRepository.save(user);
    }

    private void updateFromOAuth(User user, String provider, String providerId, boolean emailVerified, String name, String avatarUrl) {
        // Check if the user's current provider is 'LOCAL' before updating provider-specific fields
//        boolean isLocalUser = "LOCAL".equalsIgnoreCase(user.getProvider());

        user.setEmailVerified(emailVerified || user.getEmailVerified());
//        if (name != null && !name.isBlank()) {
//            if (isLocalUser) {
//                // keep local username
//            } else {
//                user.setUsername(name);
//            }
//        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setImageUrl(avatarUrl);
        }

        // Update provider and providerId only if the user is not a local user or if the provider is changing
        user.setProvider(provider);
        user.setProviderId(providerId);
    }

    private String generateUniqueUsername(String baseName, String emailFallback) {
        String base;
        if (baseName != null && !baseName.isBlank()) {
            base = normalizeUsername(baseName);
        } else if (emailFallback != null) {
            base = normalizeUsername(emailFallback.replaceAll("@.*", ""));
        } else {
            base = "user";
        }

        String candidate = base;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + new Random().nextInt(10000);
        }
        return candidate;
    }

    private String normalizeUsername(String input) {
        String normalized = input.toLowerCase(Locale.ROOT)
                .replaceAll("\\s", "_")
                .replaceAll("[^a-z0-9_]*", "");
        if (normalized.isBlank()) {
            return "user";
        }
        return normalized;
    }

    private String extractUsernameFromEmail(String input){
        return input.replaceAll("@.*", "");

    }

    private String toLower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
