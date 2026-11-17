package net.javaguides.identity_service.service;

import net.javaguides.identity_service.dto.CreateUserRequest;
import net.javaguides.identity_service.dto.PageResponse;
import net.javaguides.identity_service.dto.RoleDto;
import net.javaguides.identity_service.dto.UpdateUserRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import net.javaguides.identity_service.security.AuthUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserService {
    PageResponse<UserDto> getUsers(int page, int size);

    /** Admin creates a user with the given role codes. */
    UserDto createUser(CreateUserRequest request);

    /** Create a user with fixed roles (self-registration, Google first login). */
    UserCredential createUser(String name, String email, String rawPassword, Set<ERole> roles);

    UserDto updateUser(Long userId, UpdateUserRequest request, AuthUser actor);

    void deleteUser(Long userId, AuthUser actor);

    List<RoleDto> getRoles();

    Optional<UserCredential> findActiveWithPermissions(Long userId);

    void updateLastLoginDate(Long userId);
}
