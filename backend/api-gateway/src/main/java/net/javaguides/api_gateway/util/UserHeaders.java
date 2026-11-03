package net.javaguides.api_gateway.util;

/**
 * Headers the gateway sets for downstream services after validating the JWT.
 * Downstream services trust them, so clients must never be able to send them.
 */
public final class UserHeaders {
    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";

    private UserHeaders() {
    }
}
