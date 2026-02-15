package com.prime.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Auth Service Application.
 * Handles authentication orchestration, token issuance, refresh, and session management.
 * 
 * IMPORTANT: This service does NOT own user data. It delegates credential validation
 * to the User Service and manages authentication state (sessions, tokens).
 * 
 * Following 12-factor app methodology:
 * - Config in environment variables
 * - Stateless processes (sessions in Redis)
 * - Port binding via embedded container
 * - Disposable processes with graceful shutdown
 * - Dev/prod parity with Docker
 * - Logs as event streams
 */
@SpringBootApplication(scanBasePackages = {"com.prime.auth", "com.prime.common"})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
