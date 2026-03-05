package net.javaguides.identity_service.controller;



import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.dto.AuthRequest;
import net.javaguides.identity_service.dto.LoginResponse;
import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.entity.Permission;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.exception.AuthException;
import net.javaguides.identity_service.service.AuthService;
import net.javaguides.identity_service.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/user/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> addNewUser(@RequestBody SignUpRequest signUpRequest) {
        try {
            String message = authService.saveUser(signUpRequest);
            return new ResponseEntity<>(ApiResponse.success(message), HttpStatus.CREATED);
        }
        catch(AuthException e){
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        }
        catch(Exception e){
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<?>> getToken(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        try {
            Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
            if (authenticate.isAuthenticated()) {
                String generateToken = authService.generateToken(authRequest, response);


                UserCredential user = userService.findByUsernameWithPermissions(authRequest.getUsername()).orElseThrow();
                Set<String> permissions = user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toSet());

                // set cookie
                ResponseCookie cookie = ResponseCookie.from("token", generateToken)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());




                LoginResponse loginResponse = new LoginResponse(generateToken, permissions);
                loginResponse.setId(user.getId());
                loginResponse.setUserName(authRequest.getUsername());

                // update last login
                userService.updateLastLoginDate(user);
                return new ResponseEntity<>(ApiResponse.success(loginResponse), HttpStatus.OK);
            } else {

                //ApiResponse<String> apiResponse = new ApiResponse<>("Invalid access!");
                return new ResponseEntity<>(ApiResponse.error("Invalid access!"), HttpStatus.BAD_REQUEST);            }
        }catch(Exception e){
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateToken(@RequestParam("token") String token) {
        try {
            authService.validateToken(token);
            return new ResponseEntity<>(ApiResponse.success("Token is valid"), HttpStatus.OK);
        }catch(Exception e){
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            //UserDto userDto = userService.getUserByUsername(currentUser.getUsername());
            LoginResponse loginResponse = getUserLogin(currentUser.getUsername());
            loginResponse.setUserName(currentUser.getUsername());
            return new ResponseEntity<>(ApiResponse.success(loginResponse), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private LoginResponse getUserLogin(String userName) {
        UserCredential user = userService.findByUsernameWithPermissions(userName).orElseThrow();
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());


        LoginResponse loginResponse = new LoginResponse("", permissions);
        loginResponse.setId(user.getId());
        return loginResponse;
    }

}
