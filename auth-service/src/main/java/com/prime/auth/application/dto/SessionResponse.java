package com.prime.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.prime.auth.domain.Session;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Session info response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionResponse {

    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private boolean current;

    /**
     * Create from Session entity.
     */
    public static SessionResponse fromSession(Session session, boolean isCurrent) {
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .createdAt(session.getCreatedAt())
                .lastActivityAt(session.getLastActivityAt())
                .expiresAt(session.getExpiresAt())
                .current(isCurrent)
                .build();
    }
}
