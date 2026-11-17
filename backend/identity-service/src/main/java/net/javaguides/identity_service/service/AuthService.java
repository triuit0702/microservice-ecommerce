package net.javaguides.identity_service.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.javaguides.identity_service.dto.AuthUserResponse;
import net.javaguides.identity_service.dto.GoogleLoginRequest;
import net.javaguides.identity_service.dto.LoginRequest;
import net.javaguides.identity_service.dto.RegisterRequest;

public interface AuthService {
    /** Self-registration, always creates a CUSTOMER. */
    void register(RegisterRequest request);

    AuthUserResponse login(LoginRequest request, HttpServletResponse response);

    AuthUserResponse loginWithGoogle(GoogleLoginRequest request, HttpServletResponse response);

    /** Rotate the refresh token from the cookie and set new auth cookies. */
    void refresh(HttpServletRequest request, HttpServletResponse response);

    /** Revoke the refresh token from the cookie (if any) and clear auth cookies. */
    void logout(HttpServletRequest request, HttpServletResponse response);

    AuthUserResponse getCurrentUser(Long userId);
}
