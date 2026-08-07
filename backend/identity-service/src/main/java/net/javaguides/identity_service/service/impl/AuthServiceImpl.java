package net.javaguides.identity_service.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.config.CustomUserDetails;
import net.javaguides.identity_service.dto.AuthRequest;
import net.javaguides.identity_service.dto.CurrentUserDto;
import net.javaguides.identity_service.dto.LoginResponse;
import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.dto.cache.UserCache;
import net.javaguides.identity_service.entity.Permission;
import net.javaguides.identity_service.entity.Role;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import net.javaguides.identity_service.exception.AuthException;
import net.javaguides.identity_service.repository.RoleRepository;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import net.javaguides.identity_service.service.AuthService;
import net.javaguides.identity_service.service.JwtService;
import net.javaguides.identity_service.service.UserCacheService;
import net.javaguides.identity_service.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserCacheService userCacheService;

    @Override
    public String saveUser(SignUpRequest signUpRequest) {
        try {
            boolean existingUsername = checkExistingUsername(signUpRequest.getName());
            if(existingUsername){
                throw new AuthException("Username already exists in the database!", HttpStatus.BAD_REQUEST);
            }
            UserCredential userCredential = new UserCredential();
            userCredential.setName(signUpRequest.getName());
            userCredential.setEmail(signUpRequest.getEmail());
            if (StringUtils.isEmpty(signUpRequest.getPassword())) {
                signUpRequest.setPassword("123456");
            }
            userCredential.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
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

            userCredential.setRoles(roles);
            userCredentialRepository.save(userCredential);
            return "User added to the system!";
        }catch(Exception e){
            throw new RuntimeException("Error registering user: " + e.getMessage());
        }
    }

    @Override
    public String generateToken(AuthRequest authRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        Optional<UserCredential> optionalUser = userCredentialRepository.findByNameAndDelFlgFalse(authRequest.getUsername());
        if(!optionalUser.isPresent()){
            throw new AuthException("Invalid credentials! Please try again!",HttpStatus.UNAUTHORIZED);
        }

        UserCredential userCredential = optionalUser.get();
        SecurityContextHolder.getContext().setAuthentication(authentication);



        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String jwtToken = jwtService.generateToken(authentication);

        Cookie cookie = new Cookie("token", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 60);
        response.addCookie(cookie);
        return jwtToken;
    }

    @Override
    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    /**
     * Get user information from cache or database if not found in cache.
     *
     * @param userId the ID of the user to retrieve
     * @return the LoginResponse containing user information and permissions
     */
    @Override
    public CurrentUserDto getCurrentUser(Long userId) {

        // get user from cache
        UserCache currentUserCache = userCacheService.getCachedUser(userId);

        if (currentUserCache != null) {
            return new CurrentUserDto(
                    currentUserCache.getId(),
                    currentUserCache.getName(),
                    currentUserCache.getPermissions());
        }

        // If not found in cache, fetch from database and cache it
        UserCredential user = userService.findByUserIdWithPermission(userId).orElseThrow();
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        // Cache the user information
        UserCache userCache = userCacheService.buildUserCache(user);
        userCacheService.cacheUser(userCache);

        return new CurrentUserDto(
                user.getId(),
                user.getName(),
                permissions);
    }

    /**
     * Login user and return LoginResponse with permissions and user information.
     * @param authRequest
     * @param response
     * @return
     */
    @Override
    public LoginResponse login(AuthRequest authRequest, HttpServletResponse response) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        CustomUserDetails currentUser =
                (CustomUserDetails) authenticate.getPrincipal();

        String token = generateToken(authRequest, response);

        // set cookie
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/")
                .sameSite("None")
                .secure(true)
                .maxAge(Duration.ofMinutes(1))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // update last login
        userService.updateLastLoginDate(currentUser.getId());

        // cache user info after login success
        UserCache userCache = userCacheService.buildUserCache(currentUser);
        userCacheService.cacheUser(userCache);
        return buildLoginResponse(currentUser);
    }

    /**
     * Build LoginResponse from CustomUserDetails.
     * @param currentUser
     * @return
     */
    private LoginResponse buildLoginResponse(CustomUserDetails currentUser) {
        // tại vì đã set token trong cookie thì không cần trả token trong response body nữa, nên comment lại
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setPermissions(currentUser.getPermissions());
        //loginResponse.setToken(token);
        loginResponse.setId(currentUser.getId());
        loginResponse.setUserName(currentUser.getUsername());
        return loginResponse;
    }

    private boolean checkExistingUsername(String username){
        return userCredentialRepository.findByNameAndDelFlgFalse(username).isPresent();
    }
}
