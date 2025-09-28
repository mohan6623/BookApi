package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.dto.User;
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

    private BCryptPasswordEncoder encoder= new BCryptPasswordEncoder(12);


    public Boolean saveUser(User user) {
            user.setPassword(encoder.encode(user.getPassword()));
            return userRepo.save(user) != null;
        }


    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public JwtResponse login(User user) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(),user.getPassword()));

        // Fetch user details from DB
        User dbUser = userRepo.findByUsername(user.getUsername());
        // Map User to UserDto
        UserDto userDto = new UserDto(
                dbUser.getId(),
                dbUser.getUsername(),
                dbUser.getMail(),
                dbUser.getRole()
        );
        // Generate JWT
        String token = jwtService.generateToken(dbUser.getUsername());
        // Return response
        return new JwtResponse(token, userDto);
    }
}
