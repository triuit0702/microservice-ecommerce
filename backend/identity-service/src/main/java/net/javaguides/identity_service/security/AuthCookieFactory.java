package net.javaguides.identity_service.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.config.JwtProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/**
 * Single place for auth cookie names, paths, SameSite and lifetimes.
 */
@Component
@RequiredArgsConstructor
public class AuthCookieFactory {
    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    /** Refresh cookie is only sent to /auth/refresh and /auth/logout. */
    public static final String REFRESH_PATH = "/api/v1/user/auth";

    private final JwtProperties jwtProperties;

    public void writeAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, build(ACCESS_COOKIE, accessToken, "/", "Lax", jwtProperties.getAccessTtl()));
        addCookie(response, build(REFRESH_COOKIE, refreshToken, REFRESH_PATH, "Strict", jwtProperties.getRefreshTtl()));
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, build(ACCESS_COOKIE, "", "/", "Lax", Duration.ZERO));
        addCookie(response, build(REFRESH_COOKIE, "", REFRESH_PATH, "Strict", Duration.ZERO));
    }

    public String readAccessToken(HttpServletRequest request) {
        return readCookie(request, ACCESS_COOKIE);
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_COOKIE);
    }

    private ResponseCookie build(String name, String value, String path, String sameSite, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private static void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
