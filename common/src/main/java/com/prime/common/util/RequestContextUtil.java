package com.prime.common.util;

import jakarta.servlet.http.HttpServletRequest;

import static com.prime.common.security.SecurityConstants.*;

/**
 * Utility for extracting request context information.
 */
public final class RequestContextUtil {

    private RequestContextUtil() {
        // Prevent instantiation
    }

    /**
     * Extract client IP address from request, handling proxies.
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader(FORWARDED_FOR_HEADER);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader(REAL_IP_HEADER);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract user agent from request.
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Extract request ID, generating one if not present.
     */
    public static String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId != null ? requestId : java.util.UUID.randomUUID().toString();
    }

    /**
     * Extract correlation ID for distributed tracing.
     */
    public static String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        return correlationId != null ? correlationId : getRequestId(request);
    }

    /**
     * Extract bearer token from Authorization header.
     */
    public static String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
