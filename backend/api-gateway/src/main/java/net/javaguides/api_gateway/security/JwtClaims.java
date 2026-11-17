package net.javaguides.api_gateway.security;

/**
 * Claim names of the access token issued by identity-service ("sub" holds the user id).
 * Must stay in sync with identity-service JwtClaims.
 */
public final class JwtClaims {
    public static final String EMAIL = "email";
    public static final String ROLES = "roles";

    /** Cookie set by identity-service (AuthCookieFactory.ACCESS_COOKIE). */
    public static final String ACCESS_COOKIE = "access_token";

    private JwtClaims() {
    }
}
