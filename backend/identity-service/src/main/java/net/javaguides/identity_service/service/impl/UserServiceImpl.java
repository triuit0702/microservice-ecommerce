package net.javaguides.identity_service.service.impl;

import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.audit.Audit;
import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.entity.Role;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import net.javaguides.identity_service.exception.ResourceNotFoundException;
import net.javaguides.identity_service.repository.RoleRepository;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import net.javaguides.identity_service.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserCredentialRepository userCredentialRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;


//    public UserServiceImpl(UserCredentialRepository userCredentialRepository, RoleRepository roleRepository, ModelMapper modelMapper) {
//        this.userCredentialRepository = userCredentialRepository;
//        this.roleRepository = roleRepository;
//        this.modelMapper = modelMapper;
//    }

    @Override
    public UserDto getUserByUsername(String username) {
        UserCredential userCredential = userCredentialRepository.findByNameAndDelFlgFalse(username).orElse(null);
        if(userCredential != null){
            System.out.println("UserCredential: " + userCredential);
            return modelMapper.map(userCredential, UserDto.class);
        }
        return null;
    }

    @Override
    public Page<UserDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserCredential> users = userCredentialRepository.findByDelFlgFalse(pageable);

        return users.map(user -> {
            UserDto dto = modelMapper.map(user, UserDto.class);
            if (!CollectionUtils.isEmpty(user.getRoles())) {
                dto.setRoleId(
                        user.getRoles().stream()
                                .findFirst()
                                .map(role -> role.getId().toString())
                                .orElse(null)
                );
            }
            return dto;
        });
    }


    @Audit(action = "USER_DELETE")
    public void deleteUser(Long userId) {
        Optional<UserCredential> optUser = userCredentialRepository.findById(userId);
        if (optUser.isEmpty()) {
            throw new ResourceNotFoundException("User not found with userId: "+ userId, HttpStatus.NOT_FOUND);
        }
        UserCredential user = optUser.get();
        user.setDelFlg(true);
        userCredentialRepository.save(user);
    }

    @Audit(action = "USER_UPDATE", resourceClass = SignUpRequest.class, idField = "id")
    @Override
    public void upateUser(SignUpRequest signUpRequest) {
        // check user exist
        UserCredential user = userCredentialRepository.findById(signUpRequest.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + signUpRequest.getId(), HttpStatus.NOT_FOUND));

        // map dto  to entity except  password
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());

        // update role
        Set<Role> roles = new HashSet<>();

        if (signUpRequest.getRoleId() == null) {
            Role role = roleRepository.findByName(ERole.CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        } else {
            Role role = roleRepository.findById(Long.valueOf(signUpRequest.getRoleId()))
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        }
        user.setRoles(roles);
        userCredentialRepository.save(user);

    }

    @Override
    public Optional<UserCredential> findByUsernameWithPermissions(String username) {
        return userCredentialRepository.findByUsernameWithPermissions(username);
    }

    @Override
    public void updateLastLoginDate(UserCredential user) {
        user.setLastLoginAt(LocalDateTime.now());
        userCredentialRepository.save(user);
    }
}
