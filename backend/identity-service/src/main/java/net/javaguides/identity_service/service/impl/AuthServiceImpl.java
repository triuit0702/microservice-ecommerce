package net.javaguides.identity_service.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.dto.AuthUserResponse;
import net.javaguides.identity_service.dto.GoogleLoginRequest;
import net.javaguides.identity_service.dto.LoginRequest;
import net.javaguides.identity_service.dto.RegisterRequest;
import net.javaguides.identity_service.dto.cache.UserCache;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import net.javaguides.identity_service.exception.AuthException;
import net.javaguides.identity_service.exception.ResourceNotFoundException;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import net.javaguides.identity_service.security.AuthCookieFactory;
import net.javaguides.identity_service.security.AuthUser;
import net.javaguides.identity_service.security.CustomUserDetails;
import net.javaguides.identity_service.security.JwtService;
import net.javaguides.identity_service.security.RefreshTokenService;
import net.javaguides.identity_service.service.AuthService;
import net.javaguides.identity_service.service.GoogleOAuth2Service;
import net.javaguides.identity_service.service.UserCacheService;
import net.javaguides.identity_service.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserCredentialRepository userCredentialRepository;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserCacheService userCacheService;
    private final GoogleOAuth2Service googleOAuth2Service;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieFactory authCookieFactory;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // public self-registration: role is always CUSTOMER
        userService.createUser(request.getName(), request.getEmail(), request.getPassword(), Set.of(ERole.CUSTOMER));
    }

    @Override
    public AuthUserResponse login(LoginRequest request, HttpServletResponse response) {
        // bad credentials -> BadCredentialsException -> 401 in GlobalExceptionHandler
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        AuthUser user = ((CustomUserDetails) authentication.getPrincipal()).toAuthUser();
        return completeLogin(user, response);
    }

    /**
     * Login with Google: exchange the authorization code, find the user by verified email
     * (create a CUSTOMER account if not exists), then issue our cookies like a normal login.
     */
    @Override
    @Transactional
    public AuthUserResponse loginWithGoogle(GoogleLoginRequest request, HttpServletResponse response) {
        if (request == null || StringUtils.isEmpty(request.getCode())) {
            throw new AuthException("Google authorization code is required");
        }

        Jwt idToken = googleOAuth2Service.exchangeCodeForIdToken(request.getCode());
        String email = idToken.getClaimAsString("email");
        if (StringUtils.isEmpty(email) || !Boolean.TRUE.equals(idToken.getClaimAsBoolean("email_verified"))) {
            throw new AuthException("Google account email is not verified");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        UserCredential user = userCredentialRepository.findActiveByEmailWithPermissions(normalizedEmail)
                .orElseGet(() -> registerGoogleUser(normalizedEmail));
        return completeLogin(CustomUserDetails.build(user).toAuthUser(), response);
    }

    @Override
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieFactory.readRefreshToken(request);
        if (refreshToken == null) {
            throw new AuthException("Missing refresh token");
        }

        Long userId;
        try {
            userId = refreshTokenService.rotate(refreshToken);
        } catch (AuthException e) {
            authCookieFactory.clearAuthCookies(response);
            throw e;
        }

        // reload roles/permissions so role changes apply at the next refresh; deleted users are rejected
        UserCredential user = userService.findActiveWithPermissions(userId).orElse(null);
        if (user == null) {
            refreshTokenService.revokeAll(userId);
            authCookieFactory.clearAuthCookies(response);
            throw new AuthException("User no longer exists");
        }
        issueAuthCookies(CustomUserDetails.build(user).toAuthUser(), response);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieFactory.readRefreshToken(request);
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        authCookieFactory.clearAuthCookies(response);
    }

    /**
     * Get user info from cache, or from DB (then cache it).
     */
    @Override
    public AuthUserResponse getCurrentUser(Long userId) {
        UserCache cached = userCacheService.getCachedUser(userId);
        if (cached != null && cached.getRoles() != null) {
            return AuthUserResponse.from(cached);
        }

        UserCredential user = userService.findActiveWithPermissions(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
        AuthUser authUser = CustomUserDetails.build(user).toAuthUser();
        userCacheService.cacheUser(userCacheService.buildUserCache(authUser));
        return AuthUserResponse.from(authUser);
    }

    /**
     * Create a CUSTOMER account for a first-time Google user (random password, username from email).
     */
    private UserCredential registerGoogleUser(String email) {
        return userService.createUser(
                generateUniqueUsername(email), email, UUID.randomUUID().toString(), Set.of(ERole.CUSTOMER));
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@'));
        String username = base;
        while (userCredentialRepository.existsByName(username)) {
            username = base + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return username;
    }

    /**
     * Set cookies, update last login and cache the user after a successful login.
     */
    private AuthUserResponse completeLogin(AuthUser user, HttpServletResponse response) {
        issueAuthCookies(user, response);
        userService.updateLastLoginDate(user.id());
        userCacheService.cacheUser(userCacheService.buildUserCache(user));
        return AuthUserResponse.from(user);
    }

    private void issueAuthCookies(AuthUser user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.id());
        authCookieFactory.writeAuthCookies(response, accessToken, refreshToken);
    }
}
