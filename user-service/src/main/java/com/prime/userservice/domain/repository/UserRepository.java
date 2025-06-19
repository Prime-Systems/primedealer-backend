package com.prime.userservice.domain.repository;

import com.prime.userservice.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    //Additional query
    Optional<UserEntity> findByEmail(String email);
}
