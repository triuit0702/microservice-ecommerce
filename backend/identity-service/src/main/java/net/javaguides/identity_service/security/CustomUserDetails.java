package net.javaguides.identity_service.security;

import lombok.Getter;
import net.javaguides.identity_service.entity.Permission;
import net.javaguides.identity_service.entity.UserCredential;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Principal used only for username/password login. Requests after login use {@link AuthUser} from the token.
 */
@Getter
public class CustomUserDetails implements UserDetails {
    private final Long id;
    private final String username;
    private final String password;
    private final String email;
    private final Set<String> roles;
    private final Set<String> permissions;

    private CustomUserDetails(Long id, String username, String password, String email,
                              Set<String> roles, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles;
        this.permissions = permissions;
    }

    /**
     * Build from a user whose roles and permissions are already loaded.
     */
    public static CustomUserDetails build(UserCredential user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toUnmodifiableSet());
        // a user's permissions are the union of the permissions of all their roles
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toUnmodifiableSet());
        return new CustomUserDetails(user.getId(), user.getName(), user.getPassword(), user.getEmail(), roles, permissions);
    }

    public AuthUser toAuthUser() {
        return new AuthUser(id, username, email, roles, permissions);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Authorities.toGrantedAuthorities(roles, permissions);
    }
}
