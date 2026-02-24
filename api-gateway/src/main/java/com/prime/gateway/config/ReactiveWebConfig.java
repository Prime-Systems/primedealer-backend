package com.prime.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to ensure the gateway runs in reactive mode only.
 * This configuration is activated only for reactive web applications.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveWebConfig {
    // This configuration will only be loaded for reactive applications,
    // helping to prevent any servlet-based configurations from interfering
}
