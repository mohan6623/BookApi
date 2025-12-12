package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByMail(String mail);

    boolean existsByUsernameOrMail(String username, String mail);

    Optional<User> findByMail(String mail);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
