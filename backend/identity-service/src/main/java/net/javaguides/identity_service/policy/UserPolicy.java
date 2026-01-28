package net.javaguides.identity_service.policy;


import net.javaguides.identity_service.config.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class UserPolicy {
    public boolean canCreate(Authentication auth) {
        return hasPermission(auth, "USER_CREATE");
    }

    private boolean hasPermission(Authentication auth, String permission) {
        CustomUserDetails user =
                (CustomUserDetails) auth.getPrincipal();
        return user.hasPermission(permission);
    }
}
