package net.javaguides.identity_service.service;

import jakarta.servlet.http.HttpServletResponse;
import net.javaguides.identity_service.dto.AuthRequest;
import net.javaguides.identity_service.dto.CurrentUserDto;
import net.javaguides.identity_service.dto.LoginResponse;
import net.javaguides.identity_service.dto.SignUpRequest;

public interface AuthService {
    String saveUser(SignUpRequest userCredential);
    String generateToken(AuthRequest authRequest, HttpServletResponse response);
    CurrentUserDto getCurrentUser(Long userId);
    LoginResponse login(AuthRequest authRequest, HttpServletResponse response);
}
