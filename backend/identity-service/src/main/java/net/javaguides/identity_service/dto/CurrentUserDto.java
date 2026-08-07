package net.javaguides.identity_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserDto {

    private Long id;
    private String userName;
    private Set<String> permissions;
}
