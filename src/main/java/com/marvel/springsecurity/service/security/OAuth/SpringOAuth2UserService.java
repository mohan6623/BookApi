package com.marvel.springsecurity.service.security.OAuth;

import com.marvel.springsecurity.model.OAuthProvider;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.OAuthRepo;
import com.marvel.springsecurity.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * OAuth2 User Service for LOGIN/REGISTRATION flow only.
 * 
 * Note: OAuth CONNECTION flow (linking OAuth to existing accounts) is handled
 * by OAuthConnectionController with completely separate endpoints.
 * This service ONLY handles login/registration via OAuth.
 */
@Slf4j
@Service
public class SpringOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuthRepo oAuthRepo;

    public SpringOAuth2UserService(UserRepository userRepository, OAuthRepo oAuthRepo) {
        this.userRepository = userRepository;
        this.oAuthRepo = oAuthRepo;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        String providerId = oAuth2User.getName(); // "sub" for Google, "id" for GitHub usually
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture"); // Google specific, GitHub is "avatar_url"

        if (provider.equals("GITHUB")) {
            // GitHub specific mapping
            Integer id = oAuth2User.getAttribute("id");
            providerId = id != null ? id.toString() : providerId;
            picture = oAuth2User.getAttribute("avatar_url");
        }

        // Prepare attributes for the returned OAuth2User
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("provider_id", providerId);
        attributes.put("picture", picture);
        attributes.put("name", name);
        attributes.put("provider", provider);

        // 1. Check if user exists by Provider + ProviderId (existing OAuth login)
        Optional<OAuthProvider> existingProvider = oAuthRepo.findByProviderAndProviderId(provider, providerId);
        Users user;

        if (existingProvider.isPresent()) {
            // User logging in with existing OAuth account
            user = existingProvider.get().getUser();
            // Only set profile picture if the user doesn't have one yet (legacy support)
            if ((user.getImageUrl() == null || user.getImageUrl().isEmpty()) && picture != null) {
                user.setImageUrl(picture);
                userRepository.save(user);
            }
        } else {
            // If not found, check if email is missing
            if (email == null) {
                // Scenario B: Email missing. Return without saving to DB.
                // SuccessHandler will detect missing 'app_user_id' and trigger the pending
                // flow.
                attributes.put("email_missing", true);
                String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUserNameAttributeName();
                return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, userNameAttributeName);
            }

            // 2. Check if email already exists (Account Linking Check)
            Optional<Users> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                Users existingUser = existingUserByEmail.get();

                // If email is verified, don't allow login with different OAuth
                // User should use "Connect" feature from their profile instead
                if (existingUser.isEmailVerified()) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("account_exists",
                            "An account with email " + email
                                    + " already exists. Please login with your existing account and use the 'Connect' feature to link this OAuth provider.",
                            null));
                }

                // SECURITY: Allow overwriting unverified accounts
                log.info("Overwriting unverified account - Email: {}, Provider: {}, Existing Account Created: {}",
                        email, provider, existingUser.getCreatedAt());
                userRepository.delete(existingUser);
                userRepository.flush();
            }

            // 3. NEW USER REGISTRATION - Don't auto-create, let user choose
            // username/display name
            // Generate suggested username for the frontend
            String suggestedUsername = generateUniqueUsername(name, email);

            attributes.put("new_registration", true);
            attributes.put("suggested_username", suggestedUsername);
            attributes.put("oauth_email", email);

            String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();
            return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, userNameAttributeName);
        }

        // Prepare final OAuth2User with app-specific data (for existing users only)
        attributes.put("app_user_id", user.getUserId());
        attributes.put("app_username", user.getUsername());
        attributes.put("app_role", user.getRole());
        attributes.put("app_image_url", user.getImageUrl());

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, userNameAttributeName);
    }

    private String generateUniqueUsername(String name, String email) {
        String base = (name != null && !name.isBlank())
                ? name.toLowerCase().replaceAll("[^a-z0-9]", "")
                : email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");

        if (base.length() < 3)
            base = "user" + base;
        String candidate = base;
        int suffix = 1;

        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
