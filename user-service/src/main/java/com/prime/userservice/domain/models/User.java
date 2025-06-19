package com.prime.userservice.domain.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public record User(
        UUID id,
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @NotBlank(message = "Email is required") @Email String email,
        String phoneNumber,
        @NotBlank(message = "User role is required") UserRole role,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
