package com.prime.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic test to verify the Spring context loads without WebMVC conflicts.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTest {

    @Test
    void contextLoads() {
        // This test will fail if there are dependency conflicts
    }
}
