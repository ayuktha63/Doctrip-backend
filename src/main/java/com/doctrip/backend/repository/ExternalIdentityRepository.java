package com.doctrip.backend.repository;

import com.doctrip.backend.entity.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, String> {
    Optional<ExternalIdentity> findByProviderAndExternalId(String provider, String externalId);
}
