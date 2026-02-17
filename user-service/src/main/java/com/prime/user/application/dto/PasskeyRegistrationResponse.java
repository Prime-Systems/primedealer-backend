package com.prime.user.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Passkey registration response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasskeyRegistrationResponse {
    private UUID id;
    private String credentialId;
    private String deviceName;
    private Instant lastUsedAt;
    private Instant createdAt;
}
