package com.prime.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * User Service Application.
 * Manages user identity, credentials, MFA, passkeys, and one-time tokens.
 * 
 * Following 12-factor app methodology:
 * - Config in environment variables
 * - Stateless processes
 * - Port binding via embedded container
 * - Disposable processes with graceful shutdown
 * - Dev/prod parity with Docker
 * - Logs as event streams
 * - Admin processes as one-off tasks
 */
@SpringBootApplication(scanBasePackages = {"com.prime.user", "com.prime.common"})
@EnableJpaAuditing
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
