package com.prime.user.domain.repository;

import com.prime.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByGhanaCardNumber(String ghanaCardNumber);
    boolean existsByGhanaCardNumber(String ghanaCardNumber);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :userId")
    void resetLoginAttempts(UUID userId);

    @Query("SELECT u FROM User u JOIN u.oauthProviders op WHERE op.provider = :provider AND op.providerUserId = :providerUserId")
    Optional<User> findByOAuthProvider(String provider, String providerUserId);

    @Modifying
    @Query("UPDATE User u SET u.status = 'ACTIVE', u.emailVerified = true WHERE u.id = :userId")
    void verifyEmail(UUID userId);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :threshold AND u.status = 'ACTIVE'")
    java.util.List<User> findInactiveUsers(Instant threshold);
}
