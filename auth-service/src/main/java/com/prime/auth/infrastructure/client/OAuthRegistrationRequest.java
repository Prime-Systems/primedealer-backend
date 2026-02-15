package com.prime.auth.infrastructure.client;

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
}
