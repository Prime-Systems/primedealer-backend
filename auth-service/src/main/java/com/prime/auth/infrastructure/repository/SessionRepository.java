package com.prime.auth.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prime.auth.domain.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis-based session repository.
 * Manages authentication sessions with distributed storage.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";
    private static final String MFA_TOKEN_PREFIX = "mfa:token:";

    /**
     * Save a session.
     */
    public void save(Session session) {
        try {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            String sessionJson = objectMapper.writeValueAsString(session);
            Duration ttl = Duration.between(Instant.now(), session.getExpiresAt());

            redisTemplate.opsForValue().set(sessionKey, sessionJson, ttl);

            // Add to user's session set
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
            redisTemplate.opsForSet().add(userSessionsKey, session.getSessionId());
            redisTemplate.expire(userSessionsKey, Duration.ofDays(30));

            log.debug("Session saved: sessionId={}, userId={}", 
                    session.getSessionId(), session.getUserId());

        } catch (Exception e) {
            log.error("Error saving session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save session", e);
        }
    }

    /**
     * Find session by ID.
     */
    public Optional<Session> findById(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);

            if (sessionJson == null) {
                log.debug("Session not found: sessionId={}", sessionId);
                return Optional.empty();
            }

            Session session = objectMapper.readValue(sessionJson, Session.class);
            return Optional.of(session);

        } catch (Exception e) {
            log.error("Error finding session: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Find all sessions for a user.
     */
    public List<Session> findByUserId(UUID userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);

            if (sessionIds == null || sessionIds.isEmpty()) {
                return Collections.emptyList();
            }

            return sessionIds.stream()
                    .map(this::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(Session::isValid)
                    .sorted(Comparator.comparing(Session::getLastActivityAt).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding user sessions: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Delete a session.
     */
    public void delete(String sessionId) {
        try {
            Optional<Session> sessionOpt = findById(sessionId);
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();

                // Remove from user's session set
                String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
                redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            }

            // Delete the session
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);

            log.debug("Session deleted: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("Error deleting session: {}", e.getMessage(), e);
        }
    }

    /**
     * Delete all sessions for a user.
     */
    public void deleteAllByUserId(UUID userId) {
        List<Session> sessions = findByUserId(userId);
        sessions.forEach(session -> {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            redisTemplate.delete(sessionKey);
        });

        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        redisTemplate.delete(userSessionsKey);

        log.debug("All sessions deleted for user: userId={}", userId);
    }

    /**
     * Delete all other sessions for a user (except current).
     */
    public void deleteOthersByUserId(UUID userId, String currentSessionId) {
        List<Session> sessions = findByUserId(userId);
        sessions.stream()
                .filter(s -> !s.getSessionId().equals(currentSessionId))
                .forEach(session -> delete(session.getSessionId()));

        log.debug("Other sessions deleted for user: userId={}, kept={}", userId, currentSessionId);
    }

    /**
     * Store temporary MFA token.
     */
    public void saveMfaToken(String token, Session partialSession, Duration ttl) {
        try {
            String key = MFA_TOKEN_PREFIX + token;
            String sessionJson = objectMapper.writeValueAsString(partialSession);
            redisTemplate.opsForValue().set(key, sessionJson, ttl);
        } catch (Exception e) {
            log.error("Error saving MFA token: {}", e.getMessage(), e);
        }
    }

    /**
     * Get and remove MFA token.
     */
    public Optional<Session> consumeMfaToken(String token) {
        try {
            String key = MFA_TOKEN_PREFIX + token;
            String sessionJson = redisTemplate.opsForValue().getAndDelete(key);

            if (sessionJson == null) {
                return Optional.empty();
            }

            Session session = objectMapper.readValue(sessionJson, Session.class);
            return Optional.of(session);

        } catch (Exception e) {
            log.error("Error consuming MFA token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Count active sessions for a user.
     */
    public long countByUserId(UUID userId) {
        return findByUserId(userId).size();
    }
}
