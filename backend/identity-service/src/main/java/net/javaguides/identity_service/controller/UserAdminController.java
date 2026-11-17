package net.javaguides.identity_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.dto.CreateUserRequest;
import net.javaguides.identity_service.dto.PageResponse;
import net.javaguides.identity_service.dto.UpdateUserRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.security.AuthUser;
import net.javaguides.identity_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management for the admin app. Permission codes: see Authorities / init.sql.
 */
@RestController
@RequestMapping("api/v1/user/admin/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;

    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsers(page, size)));
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(ApiResponse.success(userService.createUser(request)), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateUserRequest request,
                                                           @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request, actor)));
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthUser actor) {
        userService.deleteUser(id, actor);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }
}
