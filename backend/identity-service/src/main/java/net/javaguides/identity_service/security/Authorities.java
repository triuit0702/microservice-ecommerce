package net.javaguides.identity_service.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Permission codes (same values as permissions.name in env/mysql/init/init.sql) and authority helpers.
 */
public final class Authorities {
    public static final String ROLE_PREFIX = "ROLE_";

    public static final String USER_READ = "user:read";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";

    public static final String PRODUCT_READ = "product:read";
    public static final String PRODUCT_CREATE = "product:create";
    public static final String PRODUCT_UPDATE = "product:update";
    public static final String PRODUCT_DELETE = "product:delete";

    private Authorities() {
    }

    /**
     * Authorities of a principal: ROLE_&lt;role&gt; for each role plus each permission code.
     */
    public static List<GrantedAuthority> toGrantedAuthorities(Collection<String> roles, Collection<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }
}
