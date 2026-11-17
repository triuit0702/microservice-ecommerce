package net.javaguides.identity_service.security;

import net.javaguides.identity_service.enums.ERole;

import java.security.Principal;
import java.util.Set;

/**
 * Authenticated user built from the access token (no DB lookup per request).
 */
public record AuthUser(Long id, String username, String email, Set<String> roles, Set<String> permissions)
        implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public boolean hasRole(ERole role) {
        return roles.contains(role.name());
    }
}
