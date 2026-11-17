package net.javaguides.identity_service.service.impl;

import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.audit.Audit;
import net.javaguides.identity_service.dto.CreateUserRequest;
import net.javaguides.identity_service.dto.PageResponse;
import net.javaguides.identity_service.dto.RoleDto;
import net.javaguides.identity_service.dto.UpdateUserRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.entity.Role;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import net.javaguides.identity_service.exception.ResourceExistException;
import net.javaguides.identity_service.exception.ResourceNotFoundException;
import net.javaguides.identity_service.repository.RoleRepository;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import net.javaguides.identity_service.security.AuthUser;
import net.javaguides.identity_service.security.RefreshTokenService;
import net.javaguides.identity_service.service.UserCacheService;
import net.javaguides.identity_service.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final int MAX_PAGE_SIZE = 100;

    private final UserCredentialRepository userCredentialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCacheService userCacheService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsers(int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), pageSize, Sort.by("id"));
        return PageResponse.from(userCredentialRepository.findByDelFlgFalse(pageable).map(UserDto::from));
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        Set<ERole> roles = parseRoleCodes(request.getRoleCodes());
        return UserDto.from(createUser(request.getName(), request.getEmail(), request.getPassword(), roles));
    }

    @Override
    @Transactional
    public UserCredential createUser(String name, String email, String rawPassword, Set<ERole> roles) {
        validateRoleCombination(roles);
        String username = name.trim();
        String normalizedEmail = normalizeEmail(email);
        if (userCredentialRepository.existsByName(username)) {
            throw new ResourceExistException("Username already exists");
        }
        // Google login matches accounts by email, so emails must be unique
        if (normalizedEmail != null && userCredentialRepository.existsByEmail(normalizedEmail)) {
            throw new ResourceExistException("Email already exists");
        }

        UserCredential user = new UserCredential();
        user.setName(username);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(findRoles(roles));
        return userCredentialRepository.save(user);
    }

    @Audit(action = "USER_UPDATE")
    @Override
    @Transactional
    public UserDto updateUser(Long userId, UpdateUserRequest request, AuthUser actor) {
        UserCredential user = getActiveUser(userId);
        String username = request.getName().trim();
        String email = normalizeEmail(request.getEmail());
        if (userCredentialRepository.existsByNameAndIdNot(username, userId)) {
            throw new ResourceExistException("Username already exists");
        }
        if (email != null && userCredentialRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ResourceExistException("Email already exists");
        }
        user.setName(username);
        user.setEmail(email);

        // null keeps the current roles; an empty list is rejected by parseRoleCodes
        boolean rolesChanged = false;
        if (request.getRoleCodes() != null) {
            Set<ERole> currentRoles = roleNames(user);
            Set<ERole> newRoles = parseRoleCodes(request.getRoleCodes());
            validateRoleCombination(newRoles);
            validateRoleChange(user, currentRoles, newRoles, actor);
            if (!currentRoles.equals(newRoles)) {
                user.setRoles(findRoles(newRoles));
                rolesChanged = true;
            }
        }
        userCredentialRepository.save(user);

        userCacheService.evictUser(userId);
        if (rolesChanged) {
            // new roles apply at the next refresh; the current access token lives at most 15 minutes
            refreshTokenService.revokeAll(userId);
        }
        return UserDto.from(user);
    }

    @Audit(action = "USER_DELETE")
    @Override
    @Transactional
    public void deleteUser(Long userId, AuthUser actor) {
        if (userId.equals(actor.id())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        UserCredential user = getActiveUser(userId);
        if (roleNames(user).contains(ERole.ADMINISTRATOR) && isLastAdministrator()) {
            throw new IllegalArgumentException("Cannot delete the last administrator");
        }

        // soft delete (orders keep the userId); free name/email for the unique constraints
        String prefix = "deleted_" + userId + "_";
        user.setName(prefix + user.getName());
        if (user.getEmail() != null) {
            user.setEmail(prefix + user.getEmail());
        }
        user.setDelFlg(true);
        userCredentialRepository.save(user);

        userCacheService.evictUser(userId);
        refreshTokenService.revokeAll(userId);
    }

    @Override
    public List<RoleDto> getRoles() {
        return Arrays.stream(ERole.values()).map(RoleDto::from).toList();
    }

    @Override
    public Optional<UserCredential> findActiveWithPermissions(Long userId) {
        return userCredentialRepository.findActiveByIdWithPermissions(userId);
    }

    @Override
    @Transactional
    public void updateLastLoginDate(Long userId) {
        UserCredential user = userCredentialRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
        user.setLastLoginAt(LocalDateTime.now());
        userCredentialRepository.save(user);
    }

    private UserCredential getActiveUser(Long userId) {
        return userCredentialRepository.findByIdAndDelFlgFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
    }

    /**
     * Parse role codes like "EMPLOYEE". Unknown codes return 404, an empty list returns 400.
     */
    private static Set<ERole> parseRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        Set<ERole> roles = EnumSet.noneOf(ERole.class);
        for (String code : roleCodes) {
            try {
                roles.add(ERole.valueOf(code.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ResourceNotFoundException("Role not found: " + code);
            }
        }
        return roles;
    }

    /**
     * Customers and staff are separate groups: CUSTOMER cannot be combined with another role.
     */
    private static void validateRoleCombination(Set<ERole> roles) {
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        if (roles.contains(ERole.CUSTOMER) && roles.size() > 1) {
            throw new IllegalArgumentException("CUSTOMER cannot be combined with other roles");
        }
    }

    private void validateRoleChange(UserCredential user, Set<ERole> currentRoles, Set<ERole> newRoles, AuthUser actor) {
        if (currentRoles.contains(ERole.CUSTOMER) != newRoles.contains(ERole.CUSTOMER)) {
            throw new IllegalArgumentException("Cannot convert between customer and staff accounts");
        }
        boolean removesAdmin = currentRoles.contains(ERole.ADMINISTRATOR) && !newRoles.contains(ERole.ADMINISTRATOR);
        if (removesAdmin && user.getId().equals(actor.id())) {
            throw new IllegalArgumentException("You cannot remove your own ADMINISTRATOR role");
        }
        if (removesAdmin && isLastAdministrator()) {
            throw new IllegalArgumentException("Cannot remove the last administrator");
        }
    }

    private boolean isLastAdministrator() {
        return userCredentialRepository.countActiveByRole(ERole.ADMINISTRATOR) <= 1;
    }

    private Set<Role> findRoles(Set<ERole> names) {
        List<Role> roles = roleRepository.findByNameIn(names);
        if (roles.size() != names.size()) {
            throw new ResourceNotFoundException("Role not found, check seed data in init.sql");
        }
        return new HashSet<>(roles);
    }

    private static Set<ERole> roleNames(UserCredential user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ERole.class)));
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
