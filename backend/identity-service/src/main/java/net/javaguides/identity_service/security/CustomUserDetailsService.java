package net.javaguides.identity_service.security;

import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads active users with roles and permissions for username/password login.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserCredentialRepository userCredentialRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userCredentialRepository.findActiveByNameWithPermissions(username)
                .map(CustomUserDetails::build)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
