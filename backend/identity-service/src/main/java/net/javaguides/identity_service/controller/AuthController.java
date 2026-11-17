package net.javaguides.identity_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.dto.AuthUserResponse;
import net.javaguides.identity_service.dto.GoogleLoginRequest;
import net.javaguides.identity_service.dto.LoginRequest;
import net.javaguides.identity_service.dto.RegisterRequest;
import net.javaguides.identity_service.security.AuthUser;
import net.javaguides.identity_service.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication for both frontends. Tokens are only sent as httpOnly cookies, never in the body.
 */
@RestController
@RequestMapping("api/v1/user/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /** Self-registration, the account is always CUSTOMER. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return new ResponseEntity<>(ApiResponse.success("User registered"), HttpStatus.CREATED);
    }

    /** Login with username/password, sets access_token and refresh_token cookies. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUserResponse>> login(@Valid @RequestBody LoginRequest request,
                                                               HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request, response)));
    }

    /** Login with Google (authorization code flow), sets auth cookies. */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthUserResponse>> loginWithGoogle(@RequestBody GoogleLoginRequest request,
                                                                         HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithGoogle(request, response)));
    }

    /** Rotate the refresh_token cookie and issue a new access_token cookie. */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        authService.refresh(request, response);
        return ResponseEntity.noContent().build();
    }

    /** Public on purpose: works even when the access token has expired. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> getCurrentUser(@AuthenticationPrincipal AuthUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser(currentUser.id())));
    }
}
