package net.javaguides.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for login with Google: authorization code returned by Google to the frontend.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginRequest {
    private String code;
}
