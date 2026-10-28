package net.javaguides.identity_service.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface GoogleOAuth2Service {
    /**
     * Exchange the authorization code with Google and return the verified ID token.
     */
    Jwt exchangeCodeForIdToken(String code);
}
