package net.javaguides.identity_service.controller;



import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.config.CustomUserDetails;
import net.javaguides.identity_service.dto.AuthRequest;
import net.javaguides.identity_service.dto.CurrentUserDto;
import net.javaguides.identity_service.dto.LoginResponse;
import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.service.AuthService;
import net.javaguides.identity_service.service.UserCacheService;
import net.javaguides.identity_service.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

//  Authen for client
@RestController
@RequestMapping("api/v1/user/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserCacheService userCacheService;

    /**
     * register new user
     * @param signUpRequest
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> addNewUser(@RequestBody SignUpRequest signUpRequest) {
        String message = authService.saveUser(signUpRequest);
        return new ResponseEntity<>(ApiResponse.success(message), HttpStatus.CREATED);
    }

    /**
     * login and generate token
     * @param authRequest
     * @param response
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(authRequest, response);
        return new ResponseEntity<>(ApiResponse.success(loginResponse), HttpStatus.OK);
    }

    /**
     * Get current user information
     * @param currentUser
     * @return
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails currentUser) {
        // get current user
        CurrentUserDto currentUserDto = authService.getCurrentUser(currentUser.getId());
        return new ResponseEntity<>(ApiResponse.success(currentUserDto), HttpStatus.OK);
    }

    /**
     * Logout user and clear the token cookie
     * @param response
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        // Xoá cookie
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) // Xoá cookie ngay lập tức
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new ResponseEntity<>(ApiResponse.success("Logged out successfully"), HttpStatus.OK);
    }

}
