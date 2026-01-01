package net.javaguides.identity_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public class LoginResponse {
    private Long id;
    private String userName;
    private String token;
    private Set<String> permissions;

    public LoginResponse(String token, Set<String> permissions) {
        this.token = token;
        this.permissions = permissions;
    }
}
