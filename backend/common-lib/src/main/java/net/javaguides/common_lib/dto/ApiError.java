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

    /** Optional machine-readable error code (e.g. PRICE_CHANGED) so clients do not parse messages. */
    private String code;

    public ApiError(String message, Map<String, String> details) {
        this.message = message;
        this.details = details;
    }
}
