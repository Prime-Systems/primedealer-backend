package com.prime.user.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.prime.common.security.UserRole;
import com.prime.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

//    @JsonIgnore
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean idVerificationStatus;
    private boolean mfaEnabled;
    private String mfaMethod;
    private Set<UserRole> roles;
    private Set<OAuthProviderResponse> oauthProviders;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Map from User entity to UserResponse DTO.
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .idVerificationStatus(user.isIdVerificationStatus())
                .mfaEnabled(user.isMfaEnabled())
                .mfaMethod(user.getMfaMethod() != null ? user.getMfaMethod().name() : null)
                .roles(new HashSet<>(user.getRoles()))
                .oauthProviders(user.getOauthProviders() != null ? 
                        user.getOauthProviders().stream()
                                .map(op -> OAuthProviderResponse.builder()
                                        .provider(op.getProvider())
                                        .providerUserId(op.getProviderUserId())
                                        .email(op.getEmail())
                                        .createdAt(op.getCreatedAt())
                                        .build())
                                .collect(java.util.stream.Collectors.toSet()) : null)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Map to a minimal response for internal service calls.
     */
    public static UserResponse minimalFromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .status(user.getStatus().name())
                .mfaEnabled(user.isMfaEnabled())
                .roles(new HashSet<>(user.getRoles()))
                .build();
    }
}
