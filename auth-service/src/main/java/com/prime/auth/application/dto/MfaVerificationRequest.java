package com.prime.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MFA verification request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerificationRequest {

    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotBlank(message = "Code is required")
    @Pattern(regexp = "^[0-9]{6}$|^[0-9]{4}-[0-9]{4}$", message = "Invalid code format")
    private String code;
}
