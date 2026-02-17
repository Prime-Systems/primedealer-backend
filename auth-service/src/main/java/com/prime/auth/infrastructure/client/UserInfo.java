package com.prime.auth.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.prime.common.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * User info DTO received from User Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfo {
    private UUID id;
    private String email;
    private String username;
    private String status;
    private boolean mfaEnabled;
    private String mfaMethod;
    private Set<UserRole> roles;
}
