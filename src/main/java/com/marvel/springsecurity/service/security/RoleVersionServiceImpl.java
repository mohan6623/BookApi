package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleVersionServiceImpl implements RoleVersionService {

    private final UserRepository userRepository;

    public RoleVersionServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isTokenRoleVersionCurrent(String username, Integer tokenRoleVersion) {
        if (username == null || tokenRoleVersion == null) {
            return false;
        }

        Users user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) {
            return false;
        }

        Integer dbRoleVersion = user.getRoleVersion();
        if (dbRoleVersion == null) {
            return tokenRoleVersion == 0; // Default to 0 if null in DB
        }

        return dbRoleVersion.intValue() == tokenRoleVersion.intValue();
    }
}
