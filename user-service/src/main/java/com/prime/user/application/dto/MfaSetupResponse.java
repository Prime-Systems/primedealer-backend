package com.prime.user.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MFA setup response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupResponse {

    private String secret;
    private String qrCodeUri;
    private List<String> backupCodes;
    private String setupToken;
}
