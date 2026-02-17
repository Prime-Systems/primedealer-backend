package com.prime.user.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthProviderResponse {
    private String provider;
    private String providerUserId;
    private String email;
    private Instant createdAt;
}
