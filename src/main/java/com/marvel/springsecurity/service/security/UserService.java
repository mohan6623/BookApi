package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.repo.CommentRepo;
import com.marvel.springsecurity.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private CommentRepo commentRepo;
    @Autowired
    private JwtService jwtService;
    @Autowired
    AuthenticationManager authenticationManager;

    private final BCryptPasswordEncoder encoder= new BCryptPasswordEncoder(12);

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
        return new UserDto(u.getId(), u.getUsername(), u.getMail(), displayRole, u.getImageBase64());
    }

    public JwtResponse login(User user) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(),user.getPassword()));

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

    public boolean updateUser(int id, User user, MultipartFile imageFile) throws IOException {
        var old = userRepo.findById(id);
        if (old.isEmpty()) return false;
        User updateUser = old.get();
        if (user.getMail() != null) updateUser.setMail(user.getMail());
        if (user.getPassword() != null) updateUser.setPassword(encoder.encode(user.getPassword()));
        if (imageFile != null){
            System.out.println("image is saving");
            updateUser.setImageName(imageFile.getOriginalFilename());
            updateUser.setImageType(imageFile.getContentType());
            updateUser.setProfilePic(imageFile.getBytes());
            System.out.println("Image : "+ user.getPassword());
        }
        userRepo.save(updateUser);
        return true;
    }

    public boolean usernameAvailable(String username) {
        return !userRepo.existsByUsername(username.toLowerCase());
    }

    public boolean mailAvailable(String mail) {
        return !userRepo.existsByMail(mail.toLowerCase());
    }

    public boolean usernameAndMailAvailable(String username, String mail){
        System.out.println("mail : " + mail);
        return !userRepo.existsByUsernameOrMail(username.toLowerCase(), mail.toLowerCase());
    }
//    public void deleteUser(int id) {
//        commentRepo.deleteByUserId(id);
//        userRepo.deleteById(id);
//    }
}
