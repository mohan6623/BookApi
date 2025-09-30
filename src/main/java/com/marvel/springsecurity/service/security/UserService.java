package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private JwtService jwtService;
    @Autowired
    AuthenticationManager authenticationManager;

    private final BCryptPasswordEncoder encoder= new BCryptPasswordEncoder(12);


    public Boolean saveUser(User user) {
            user.setPassword(encoder.encode(user.getPassword()));
            return userRepo.save(user) != null;
        }

    private static UserDto toDto(User u) {
        String displayRole = u.getRole();
        if (displayRole != null && displayRole.startsWith("ROLE_")) {
            displayRole = displayRole.substring(5);
        }
        return new UserDto(u.getId(), u.getUsername(), u.getMail(), displayRole);
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
        String token = jwtService.generateToken(dbUser.getUsername(), role, roleVersion);
        return new JwtResponse(token, toDto(dbUser));
    }
}
