package net.javaguides.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.javaguides.identity_service.enums.ERole;

/**
 * Role option for the admin UI: code is sent back in roleCodes, label is for display.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto {
    private String code;
    private String label;

    public static RoleDto from(ERole role) {
        return new RoleDto(role.name(), role.getLabel());
    }
}
