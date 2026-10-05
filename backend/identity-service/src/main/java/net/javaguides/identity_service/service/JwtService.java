package net.javaguides.identity_service.service;

import org.springframework.security.core.Authentication;

import java.util.Set;

public interface JwtService {
    String generateToken(Authentication authentication);
}
