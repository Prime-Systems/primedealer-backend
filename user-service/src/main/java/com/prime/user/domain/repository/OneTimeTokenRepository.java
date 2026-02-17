package com.prime.user.domain.repository;

import com.prime.user.domain.entity.OneTimeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OneTimeToken entity operations.
 */
@Repository
public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {

    Optional<OneTimeToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(
            String tokenHash, Instant now);

    Optional<OneTimeToken> findByTokenHashAndTokenTypeAndUsedFalseAndExpiresAtAfter(
            String tokenHash, OneTimeToken.TokenType tokenType, Instant now);

    @Modifying
    @Query("DELETE FROM OneTimeToken t WHERE t.expiresAt < :threshold OR t.used = true")
    int cleanupExpiredTokens(Instant threshold);

    @Modifying
    @Query("UPDATE OneTimeToken t SET t.used = true, t.usedAt = :now WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.used = false")
    void invalidateUserTokensByType(UUID userId, OneTimeToken.TokenType tokenType, Instant now);

    boolean existsByUserIdAndTokenTypeAndUsedFalseAndExpiresAtAfter(
            UUID userId, OneTimeToken.TokenType tokenType, Instant now);
}
