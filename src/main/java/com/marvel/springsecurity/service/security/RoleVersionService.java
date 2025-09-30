package com.marvel.springsecurity.service.security;

public interface RoleVersionService {
    boolean isTokenRoleVersionCurrent(String username, Integer tokenRoleVersion);
}
