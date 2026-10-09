package com.microservice.sso;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SsoUserRepository extends JpaRepository<SsoUser, Long> {
    Optional<SsoUser> findByEmail(String email);
    Optional<SsoUser> findByProviderId(String providerId);
}
