package net.javaguides.common_lib.dto;

import java.util.Map;

public record ErrorDetail(String message, Map<String, String> errors) {
}
