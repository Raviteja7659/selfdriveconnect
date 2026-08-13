package com.selfdriveconnect.paymentservice.security;

import java.util.List;

public record AuthenticatedUser(Long userId, String email, List<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains("ROLE_" + role);
    }
}
