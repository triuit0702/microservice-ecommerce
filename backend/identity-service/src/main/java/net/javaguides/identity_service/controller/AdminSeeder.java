package net.javaguides.identity_service.controller;


import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.entity.Role;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import net.javaguides.identity_service.repository.RoleRepository;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner  {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {

//        UserCredential userCredential = new UserCredential();
//        userCredential.setName("admin");
//        userCredential.setEmail("admin123@gmail.com");
//
//        userCredential.setPassword(passwordEncoder.encode("123456"));
//        Set<Role> roles = new HashSet<>();
//        java.util.Optional<Role> optRole  =roleRepository.findByName(ERole.ADMINISTRATOR);
//        // get role
//        if (optRole.isPresent()) {
//            roles.add(optRole.get());
//        } else {
//            throw new RuntimeException("Role not found: " + ERole.ADMINISTRATOR);
//        }
//
//        userCredential.setRoles(roles);
//        userCredentialRepository.save(userCredential);
    }
}
