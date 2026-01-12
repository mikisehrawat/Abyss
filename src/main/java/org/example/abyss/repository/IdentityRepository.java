package org.example.abyss.repository;

import org.example.abyss.domain.Identity;
import org.example.abyss.domain.ProviderType;
import org.example.abyss.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<Identity, Long> {
    Optional<Identity> findByProviderAndProviderId(ProviderType provider, String providerId);
    List<Identity> findByUser(User user);
    Boolean existsByProviderAndProviderId(ProviderType provider, String providerId);
}