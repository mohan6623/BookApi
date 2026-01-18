package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.UserRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MyUserDetailService implements UserDetailsService {

    @Autowired
    UserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Check if input is an email or username
        Optional<Users> userOpt = repo.findByUsername(username.toLowerCase());

        if (userOpt.isEmpty()) {
            userOpt = repo.findByEmail(username.toLowerCase());
        }

        Users user = userOpt.orElse(null);
        if (user == null) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException("User 404");
        }
        return new UserPrincipal(user);
    }
}
