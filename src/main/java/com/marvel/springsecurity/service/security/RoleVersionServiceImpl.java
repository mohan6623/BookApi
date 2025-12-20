package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleVersionServiceImpl implements RoleVersionService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isTokenRoleVersionCurrent(String username, Integer tokenRoleVersion) {
        if (username == null || tokenRoleVersion == null) {
            return false;
        }

        Users user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }

        return user.getRoleVersion() == tokenRoleVersion.intValue();
    }
}
