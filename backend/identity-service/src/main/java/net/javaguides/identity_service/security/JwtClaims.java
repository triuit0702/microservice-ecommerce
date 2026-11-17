package net.javaguides.identity_service.security;

/**
 * Custom claim names of the access token. "sub" holds the user id.
 * Must stay in sync with api-gateway JwtClaims.
 */
public final class JwtClaims {
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String ROLES = "roles";
    public static final String PERMISSIONS = "perms";

    private JwtClaims() {
    }
}
