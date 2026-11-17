package net.javaguides.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.javaguides.identity_service.dto.cache.UserCache;
import net.javaguides.identity_service.security.AuthUser;

import java.util.Set;

/**
 * Body of login, Google login and /auth/me. Tokens are only sent in httpOnly cookies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {
    private Long id;
    private String userName;
    private String email;
    private Set<String> roles;
    private Set<String> permissions;

    public static AuthUserResponse from(AuthUser user) {
        return new AuthUserResponse(user.id(), user.username(), user.email(), user.roles(), user.permissions());
    }

    public static AuthUserResponse from(UserCache user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRoles(), user.getPermissions());
    }
}
