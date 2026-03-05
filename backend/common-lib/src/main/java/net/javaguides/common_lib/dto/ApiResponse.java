package net.javaguides.common_lib.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    // Getters và Setters
    private LocalDateTime timestamp  = LocalDateTime.now();
   // private String message;
    private T data;
    private ApiError error;

//    public ApiResponse(T data) {
//        this.timestamp = LocalDateTime.now();
//        this.data = data;
//        //this.statusCode = statusCode;
//    }
//    private ApiResponse(boolean success, T data, ErrorDetail error) {
//        this.success = success;
//        this.data = data;
//        this.error = error;
//    }
//
//    public ApiResponse( String message, T data, Map<String, String> errors) {
//        this.timestamp = LocalDateTime.now();
//        this.message = message;
//        this.data = data;
//        this.errors = errors;
//    }

    private ApiResponse( T data, ApiError error) {
        this.data = data;
        this.error = error;
    }
    // ===== SUCCESS =====
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>( data, null);
    }

    // ===== ERROR =====
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null,
                new ApiError(message, null));
    }

    public static <T> ApiResponse<T> error(String message,
                                           Map<String, String> validationErrors) {
        return new ApiResponse<>(null,
                new ApiError(message, validationErrors));
    }
    /**
     * create ApiResponse success with data only
     * @param data
     * @return
     * @param <T>
     */
//    public static <T> ApiResponse<T> success(T data) {
//        return new ApiResponse<>(null, data, null);
//    }
//
//    /**
//     * create ApiResponse success  with message and data
//     * @param message
//     * @param data
//     * @return
//     * @param <T>
//     */
//    public static <T> ApiResponse<T> success(String message, T data) {
//        return new ApiResponse<>( message, data, null);
//    }
//
//    public static <T> ApiResponse<T> error(String message) {
//        return new ApiResponse<>(message, null, null);
//    }
//
//    public static <T> ApiResponse<T> validation(String message, Map<String, String> errors) {
//        return new ApiResponse<>( message, null, errors);
//    }
}
