package com.prime.user.domain.repository;

import com.prime.user.domain.entity.Passkey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Passkey entity operations.
 */
@Repository
public interface PasskeyRepository extends JpaRepository<Passkey, UUID> {

    Optional<Passkey> findByCredentialIdAndRevokedFalse(String credentialId);

    List<Passkey> findByUserIdAndRevokedFalse(UUID userId);

    @Query("SELECT p FROM Passkey p WHERE p.user.id = :userId AND p.revoked = false ORDER BY p.lastUsedAt DESC NULLS LAST")
    List<Passkey> findActivePasskeysByUserId(UUID userId);

    boolean existsByCredentialId(String credentialId);

    long countByUserIdAndRevokedFalse(UUID userId);
}
