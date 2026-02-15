package com.prime.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base domain event following event-driven architecture principles.
 * Events are immutable facts about what happened in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    private String eventType;
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private String aggregateId;
    private String aggregateType;
    private String correlationId;
    private String causationId;
    private String source;
    private Map<String, Object> payload;
    private Map<String, String> metadata;

    /**
     * Create a user-related event.
     */
    public static DomainEvent userEvent(String eventType, String userId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventType(eventType)
                .aggregateId(userId)
                .aggregateType("User")
                .payload(payload)
                .source("user-service")
                .build();
    }

    /**
     * Create an authentication-related event.
     */
    public static DomainEvent authEvent(String eventType, String userId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventType(eventType)
                .aggregateId(userId)
                .aggregateType("Authentication")
                .payload(payload)
                .source("auth-service")
                .build();
    }

    /**
     * Create a security-related event.
     */
    public static DomainEvent securityEvent(String eventType, String userId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventType(eventType)
                .aggregateId(userId)
                .aggregateType("Security")
                .payload(payload)
                .source("security")
                .build();
    }
}
