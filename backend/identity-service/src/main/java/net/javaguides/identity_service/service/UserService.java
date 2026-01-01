package net.javaguides.identity_service.service;

import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.entity.UserCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserDto getUserByUsername(String username);
    Page<UserDto> getAllUsers(int page, int size);
    void deleteUser(Long userId);
    void upateUser(SignUpRequest signUpRequest);
    Optional<UserCredential> findByUsernameWithPermissions(String username);
    void updateLastLoginDate(UserCredential user);
}
