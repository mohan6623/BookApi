package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.PasswordResetDto;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.exception.ForbiddenException;
import com.marvel.springsecurity.exception.ResourceNotFoundException;
import com.marvel.springsecurity.exception.UnauthorizedException;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.book.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ImageService imageService;
    private final EmailService emailService;

    public UserService(UserRepository userRepo, JwtService jwtService, AuthenticationManager authenticationManager,
            ImageService imageService, EmailService emailService) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.imageService = imageService;
        this.emailService = emailService;
    }

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public void saveUser(Users user) {
        user.setUsername(user.getUsername().toLowerCase());
        user.setPassword(encoder.encode(user.getPassword()));
        userRepo.save(user);
    }

    public JwtResponse login(Users user) {
        // authenticating user by checking username and password(will get decoded)
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

        // Fetch user details from DB
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Users dbUser = principal.getUser();
        if (!dbUser.isEmailVerified()) {
            log.warn("Email Not verified: {}", dbUser.getEmail());
            return null;
        }
        dbUser.setVerificationToken(null);
        String role = dbUser.getRole();
        if (role == null) {
            role = "ROLE_USER"; // Default fallback
        } else if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        int roleVersion = dbUser.getRoleVersion();
        String token = jwtService.generateToken(dbUser.getUsername(), role, roleVersion, dbUser.getUserId());
        String refreshToken = jwtService.generateRefreshToken(dbUser.getUsername());

        return JwtResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(dbUser.toDto())
                .build();
    }

    public UserDto getUserProfile(int userId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not found"));
        return user.toDto();
    }

    public UserDto updateUser(int id, Users user, MultipartFile imageFile) throws IOException {
        var old = userRepo.findById(id);
        if (old.isEmpty())
            return null;
        Users updateUser = old.get();
        if (user.getUsername() != null)
            updateUser.setUsername(user.getUsername());
        if (user.getEmail() != null) {
            String token = jwtService.generateEmailToken(user.getEmail());
            updateUser.setEmailVerified(false);
            emailService.sendVerificationEmail(user.getEmail(), token);
            updateUser.setEmail(user.getEmail());
        }
        if (user.getPassword() != null)
            updateUser.setPassword(encoder.encode(user.getPassword()));
        if (imageFile != null) {
            Map<String, Object> imageInfo = imageService.uploadImage(imageFile, "profile");
            updateUser.setImageProperties(imageInfo);
        }
        return userRepo.save(updateUser).toDto();
    }

    public JwtResponse saveVerifiedUser(Users user) {
        user.setVerificationToken(null);
        user.setEmailVerified(true);
        Users savedUser = userRepo.save(user);
        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getRoleVersion(),
                user.getUserId());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        return JwtResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(savedUser.toDto())
                .build();

    }

    // direct password update using the old password
    public void updatePassword(PasswordResetDto resetDto) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new UnauthorizedException("User not Authenticated");
        }

        Users existingUser = userPrincipal.getUser();

        // Check if user has an existing password (OAuth users might have empty password)
        boolean hasPassword = existingUser.getPassword() != null && !existingUser.getPassword().isEmpty();

        if (hasPassword) {
            // Regular password update - requires old password verification
            if (resetDto.getOldPassword() == null || resetDto.getOldPassword().isBlank()) {
                throw new UnauthorizedException("Enter a valid password");
            }
            if (!encoder.matches(resetDto.getOldPassword(), existingUser.getPassword())) {
                throw new UnauthorizedException("Enter correct password");
            }
        } else {
            // OAuth account setting first password - no old password required
            log.info("OAuth user setting first password: {}", existingUser.getUsername());
        }

        existingUser.setPassword(encoder.encode(resetDto.getPassword()));
        userRepo.save(existingUser);
    }

    public JwtResponse updateUsername(int userId, UserDto userDto) {
        Users existingUser = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not found"));
        existingUser.setUsername(userDto.getUsername());
        Users savedUser = userRepo.save(existingUser);

        // Generate new tokens since username (subject) changed
        String token = jwtService.generateToken(savedUser.getUsername(), savedUser.getRole(),
                savedUser.getRoleVersion(), savedUser.getUserId());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getUsername());

        return JwtResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(savedUser.toDto())
                .build();
    }

    public UserDto updateName(int userId, UserDto userDto) {
        Users existingUser = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not found"));
        existingUser.setName(userDto.getName());
        return userRepo.save(existingUser).toDto();
    }

    public UserDto updateProfilePic(int userId, MultipartFile image) throws IOException {
        Users existingUser = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        if (image == null) {
            throw new IllegalArgumentException("Image file cannot be null");
        }
        Map<String, Object> imageInfo = imageService.uploadImage(image, "profile");
        existingUser.setImageProperties(imageInfo);
        userRepo.save(existingUser);

        return existingUser.toDto();
    }

    public boolean usernameAvailable(String username) {
        return !userRepo.existsByUsername(username.toLowerCase());
    }

    public boolean emailAvailable(String email) {
        return !userRepo.existsByEmail(email.toLowerCase());
    }

    public Users findByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }

    public boolean usernameAndMailAvailable(String username, String mail) {
        return !userRepo.existsByUsernameOrEmail(username.toLowerCase(), mail.toLowerCase());
    }

    public void save(Users existingUser) {
        userRepo.save(existingUser);
    }

    public JwtResponse JwtFromRefreshToken(JwtResponse response) {
        String refreshToken = response.getRefreshToken();

        // Validate it's actually a refresh token (not an access token)
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new ForbiddenException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsernameFromRefreshToken(refreshToken);
        if (username == null) {
            throw new ForbiddenException("Invalid refresh token");
        }

        Users dbUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new ForbiddenException("User not found"));

        String newAccessToken = jwtService.generateToken(
                dbUser.getUsername(),
                dbUser.getRole(),
                dbUser.getRoleVersion(),
                dbUser.getUserId());

        return JwtResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken) // Return the SAME refresh token
                .user(dbUser.toDto())
                .build();
    }

    // TODO : Logging
    // Missing Logs:
    // Failed login attempts
    // Failed verification attempts
    // Password reset requests
    // Account lockout events
    // Suspicious rate limit violations

}
