package com.prime.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of user roles within the system.
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {
    ADMIN(SecurityConstants.ROLE_ADMIN),
    DEALER(SecurityConstants.ROLE_DEALER),
    AGENT(SecurityConstants.ROLE_AGENT),
    BUYER(SecurityConstants.ROLE_BUYER),
    SHIPPING_AGENT(SecurityConstants.ROLE_SHIPPING_AGENT),
    PUBLIC_BUYER(SecurityConstants.ROLE_PUBLIC_BUYER);

    private final String roleName;

    /**
     * Get UserRole from role name string.
     */
    public static UserRole fromString(String roleName) {
        for (UserRole role : UserRole.values()) {
            if (role.roleName.equalsIgnoreCase(roleName) || role.name().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleName);
    }
}
