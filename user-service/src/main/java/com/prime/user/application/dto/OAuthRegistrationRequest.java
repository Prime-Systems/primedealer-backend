package com.prime.user.application.dto;

import com.prime.common.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthRegistrationRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String provider;
    private String providerUserId;
    private UserRole role;
}
