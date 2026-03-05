package net.javaguides.common_lib.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {

    private String message;
    private Map<String, String> details;
}
