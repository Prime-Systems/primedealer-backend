package com.prime.userservice.web.dto.request;

import com.prime.userservice.domain.model.UserRole;

public record CreateUserRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        UserRole role,
        String phone,
        boolean isActive
) {
}
