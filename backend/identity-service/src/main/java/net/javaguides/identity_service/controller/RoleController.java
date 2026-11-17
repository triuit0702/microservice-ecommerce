package net.javaguides.identity_service.controller;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.dto.RoleDto;
import net.javaguides.identity_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Role options for the admin user form (staff only, see AuthConfig).
 */
@RestController
@RequestMapping("api/v1/user/roles")
@RequiredArgsConstructor
public class RoleController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> getRoles() {
        return ResponseEntity.ok(ApiResponse.success(userService.getRoles()));
    }
}
