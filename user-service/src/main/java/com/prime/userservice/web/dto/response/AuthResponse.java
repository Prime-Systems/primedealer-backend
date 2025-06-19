package com.prime.userservice.web.dto.response;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String token,
        String message
) {
}
