package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.repo.CommentRepo;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.book.ImageService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class UserService {


    private final UserRepository userRepo;
    private final CommentRepo commentRepo;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ImageService imageService;

    public UserService(UserRepository userRepo, CommentRepo commentRepo, JwtService jwtService, AuthenticationManager authenticationManager, ImageService imageService) {
        this.userRepo = userRepo;
        this.commentRepo = commentRepo;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.imageService = imageService;
    }

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public Boolean saveUser(User user) {
        try {
            user.setPassword(encoder.encode(user.getPassword()));
            userRepo.save(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static UserDto toDto(User u) {
        String displayRole = u.getRole();
        if (displayRole != null && displayRole.startsWith("ROLE_")) {
            displayRole = displayRole.substring(5);
        }
        return new UserDto(u.getId(), u.getUsername(), u.getMail(), displayRole, u.getImagePublicId(), u.getImageUrl());
    }

    public JwtResponse login(User user) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

        // Fetch user details from DB
        User dbUser = userRepo.findByUsername(user.getUsername());
        String role = dbUser.getRole();
        if (role != null && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        int roleVersion = dbUser.getRoleVersion();
        String token = jwtService.generateToken(dbUser.getUsername(), role, roleVersion, dbUser.getId());
        return new JwtResponse(token, toDto(dbUser));
    }

    public UserDto updateUser(int id, User user, MultipartFile imageFile) throws IOException {
        var old = userRepo.findById(id);
        if (old.isEmpty()) return null;
        User updateUser = old.get();
        if(user.getUsername() != null) updateUser.setUsername(user.getUsername());
        if (user.getMail() != null) updateUser.setMail(user.getMail());
        if (user.getPassword() != null) updateUser.setPassword(encoder.encode(user.getPassword()));
        if (imageFile != null) {
            Map<String, Object> imageInfo = imageService.uploadImage(imageFile, "profile");
            updateUser.setImagePublicId((String) imageInfo.get("public_id"));
            updateUser.setImageUrl((String) imageInfo.get("secure_url"));
        }
        return toDto(userRepo.save(updateUser));
    }

    public boolean usernameAvailable(String username) {
        return !userRepo.existsByUsername(username.toLowerCase());
    }

    public boolean mailAvailable (String mail){
        return !userRepo.existsByMail(mail.toLowerCase());
    }

    public boolean usernameAndMailAvailable (String username, String mail){
        return !userRepo.existsByUsernameOrMail(username.toLowerCase(), mail.toLowerCase());
    }
}
