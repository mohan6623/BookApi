package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.Users;
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
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ImageService imageService;

    public UserService(UserRepository userRepo, CommentRepo commentRepo, JwtService jwtService, AuthenticationManager authenticationManager, ImageService imageService) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.imageService = imageService;
    }

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public void saveUser(Users user) {
            user.setPassword(encoder.encode(user.getPassword()));
            userRepo.save(user);
    }



    public JwtResponse login(Users user) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

        // Fetch user details from DB
        Users dbUser = userRepo.findByUsername(user.getUsername());
        String role = dbUser.getRole();
        if (role != null && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        int roleVersion = dbUser.getRoleVersion();
        String token = jwtService.generateToken(dbUser.getUsername(), role, roleVersion, dbUser.getUserId());

        return JwtResponse.builder()
                .token(token)
                .user(dbUser.toDto())
                .build();
    }

    public UserDto updateUser(int id, Users user, MultipartFile imageFile) throws IOException {
        var old = userRepo.findById(id);
        if (old.isEmpty()) return null;
        Users updateUser = old.get();
        if(user.getUsername() != null) updateUser.setUsername(user.getUsername());
        if (user.getEmail() != null) updateUser.setEmail(user.getEmail());
        if (user.getPassword() != null) updateUser.setPassword(encoder.encode(user.getPassword()));
        if (imageFile != null) {
            Map<String, Object> imageInfo = imageService.uploadImage(imageFile, "profile");
            updateUser.setImagePublicId((String) imageInfo.get("public_id"));
            updateUser.setImageUrl((String) imageInfo.get("secure_url"));
        }
        return userRepo.save(updateUser).toDto();
    }


    public JwtResponse saveVerifiedUser(Users user) {
        user.setVerificationToken(null);
        user.setEmailVerified(true);
        Users savedUser = userRepo.save(user);
        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getRoleVersion(), user.getUserId());
        return JwtResponse.builder()
                .token(token)
                .user(savedUser.toDto())
                .build();

    }

    public boolean usernameAvailable(String username) {
        return !userRepo.existsByUsername(username.toLowerCase());
    }

    public boolean mailAvailable (String mail){
        return !userRepo.existsByEmail(mail.toLowerCase());
    }

    public Users findByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }

    public boolean usernameAndMailAvailable (String username, String mail){
        return !userRepo.existsByUsernameOrEmail(username.toLowerCase(), mail.toLowerCase());
    }
}
