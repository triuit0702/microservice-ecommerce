package net.javaguides.identity_service.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.config.CustomUserDetails;
import net.javaguides.identity_service.dto.AuthRequest;
import net.javaguides.identity_service.dto.CurrentUserDto;
import net.javaguides.identity_service.dto.GoogleLoginRequest;
import net.javaguides.identity_service.dto.LoginResponse;
import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.dto.cache.UserCache;
import net.javaguides.identity_service.entity.Permission;
import net.javaguides.identity_service.entity.Role;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import net.javaguides.identity_service.exception.AuthException;
import net.javaguides.identity_service.exception.ResourceExistException;
import net.javaguides.identity_service.exception.ResourceNotFoundException;
import net.javaguides.identity_service.repository.RoleRepository;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import net.javaguides.identity_service.service.AuthService;
import net.javaguides.identity_service.service.GoogleOAuth2Service;
import net.javaguides.identity_service.service.JwtService;
import net.javaguides.identity_service.service.UserCacheService;
import net.javaguides.identity_service.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
    private final GoogleOAuth2Service googleOAuth2Service;

    @Override
    @Transactional
    public String saveUser(SignUpRequest signUpRequest) {
        boolean existingUsername = checkExistingUsername(signUpRequest.getName());
        if (existingUsername) {
            throw new ResourceExistException("Username already exists in the DB!");
        }
        UserCredential userCredential = new UserCredential();
        userCredential.setName(signUpRequest.getName());
        userCredential.setEmail(signUpRequest.getEmail());
        // TODO: hard code temporary
        if (StringUtils.isEmpty(signUpRequest.getPassword())) {
            signUpRequest.setPassword("123456");
        }
        userCredential.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        Set<Role> roles = new HashSet<>();

        // get role
        roles.add(getRole(signUpRequest.getRoleId()));

        userCredential.setRoles(roles);
        userCredentialRepository.save(userCredential);
        return "User added to the system!";
    }

    /**
     * Get role by roleId. If roleId is null, return default role CUSTOMER.
     * @param roleId
     * @return
     */
    private Role getRole(Integer roleId) {
        if (roleId == null) {
            return roleRepository.findByName(ERole.CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        }
        return roleRepository.findById(Long.valueOf(roleId))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    @Override
    public String generateToken(AuthRequest authRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        Optional<UserCredential> optionalUser = userCredentialRepository.findByNameAndDelFlgFalse(authRequest.getUsername());
        if(optionalUser.isEmpty()){
            throw new AuthException("Invalid credentials! Please try again!");
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
        addTokenCookie(response, token);

        return completeLogin(currentUser);
    }

    /**
     * Login with Google: exchange authorization code, find user by verified email
     * (create a CUSTOMER account if not exists), then issue our JWT cookie like normal login.
     * @param googleLoginRequest
     * @param response
     * @return
     */
    @Override
    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest googleLoginRequest, HttpServletResponse response) {
        if (googleLoginRequest == null || StringUtils.isEmpty(googleLoginRequest.getCode())) {
            throw new AuthException("Google authorization code is required");
        }

        Jwt idToken = googleOAuth2Service.exchangeCodeForIdToken(googleLoginRequest.getCode());
        String email = idToken.getClaimAsString("email");
        if (StringUtils.isEmpty(email) || !Boolean.TRUE.equals(idToken.getClaimAsBoolean("email_verified"))) {
            throw new AuthException("Google account email is not verified");
        }

        UserCredential user = userCredentialRepository.findFirstByEmailAndDelFlgFalse(email)
                .orElseGet(() -> registerGoogleUser(email));

        CustomUserDetails currentUser = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        addTokenCookie(response, jwtService.generateToken(authentication));

        return completeLogin(currentUser);
    }

    /**
     * Create a CUSTOMER account for a first-time Google user (random password, username from email).
     */
    private UserCredential registerGoogleUser(String email) {
        UserCredential userCredential = new UserCredential();
        userCredential.setName(generateUniqueUsername(email));
        userCredential.setEmail(email);
        userCredential.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        Set<Role> roles = new HashSet<>();
        roles.add(getRole(null));
        userCredential.setRoles(roles);
        return userCredentialRepository.save(userCredential);
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@'));
        String username = base;
        while (userCredentialRepository.existsByName(username)) {
            username = base + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return username;
    }

    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/")
                .sameSite("None")
                .secure(true)
                .maxAge(Duration.ofMinutes(15))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Update last login, cache user info and build response after login success.
     */
    private LoginResponse completeLogin(CustomUserDetails currentUser) {
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
