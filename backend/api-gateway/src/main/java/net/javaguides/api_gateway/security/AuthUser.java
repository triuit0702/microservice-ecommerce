package net.javaguides.api_gateway.security;

import java.util.Set;

/**
 * User from a verified access token. Stored in the exchange attribute {@link #ATTRIBUTE}
 * by AuthenticationFilter so later filters do not parse the token again.
 */
public record AuthUser(Long id, String email, Set<String> roles) {
    public static final String ATTRIBUTE = AuthUser.class.getName();
}
