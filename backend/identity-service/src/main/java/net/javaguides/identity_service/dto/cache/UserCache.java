package net.javaguides.identity_service.dto.cache;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCache {
    private Long id;
    private String name;
    private String email;

    private Set<String> permissions;
}
