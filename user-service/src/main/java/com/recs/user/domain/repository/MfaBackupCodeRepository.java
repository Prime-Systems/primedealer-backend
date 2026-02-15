package com.prime.user.domain.repository;

import com.prime.user.domain.entity.MfaBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MfaBackupCode entity operations.
 */
@Repository
public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, UUID> {

    List<MfaBackupCode> findByUserIdAndUsedFalse(UUID userId);

    Optional<MfaBackupCode> findByUserIdAndCodeHashAndUsedFalse(UUID userId, String codeHash);

    long countByUserIdAndUsedFalse(UUID userId);

    @Modifying
    @Query("DELETE FROM MfaBackupCode m WHERE m.user.id = :userId")
    void deleteByUserId(UUID userId);
}
