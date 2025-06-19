package com.prime.userservice.web.dto.response;

public record CreateUserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean isActive
) {
}
