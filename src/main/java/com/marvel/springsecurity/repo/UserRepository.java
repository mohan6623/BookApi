package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {

    Users findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String mail);

    boolean existsByUsernameOrEmail(String username, String mail);

    Optional<Users> findByEmail(String mail);
}
