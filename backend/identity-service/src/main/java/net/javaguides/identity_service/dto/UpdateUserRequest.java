package net.javaguides.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Admin updates a user. roleCodes = null keeps the current roles, an empty list is rejected.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be at most 50 characters")
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;
    private List<String> roleCodes;
}
