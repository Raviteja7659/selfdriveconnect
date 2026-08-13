package com.selfdriveconnect.vehicleservice.security;

import java.util.List;

/**
 * Represents whoever the JWT belongs to. Built entirely from token
 * claims — vehicle-service never queries user-service or a users table
 * to authenticate a request; it just trusts a correctly-signed token.
 */
public record AuthenticatedUser(Long userId, String email, List<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains("ROLE_" + role);
    }
}
