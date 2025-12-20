package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthRepo extends JpaRepository<OAuthProvider, Integer> {


    boolean existsByProviderId(String providerId);

    boolean existsByProviderIdAndProvider(String providerId, String provider);

    Optional<OAuthProvider> findByProviderAndProviderId(String google, String providerId);
}
