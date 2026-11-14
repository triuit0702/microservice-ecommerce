package net.javaguides.order_service.exception;

import lombok.extern.slf4j.Slf4j;
import net.javaguides.common_lib.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps exceptions from order-service controllers to ApiResponse errors. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ApiResponse<?>> handleOrderException(OrderException e) {
        Map<String, String> details = e instanceof PriceChangedException priceChanged ? priceChanged.getDetails() : null;
        ApiResponse<?> body = ApiResponse.error(e.getMessage(), details);
        body.getError().setCode(e.getCode());
        return new ResponseEntity<>(body, e.getStatus());
    }

    /** @Valid request body errors: details map field -> message (first error per field). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ApiResponse<?> body = ApiResponse.error("Invalid order information", fieldErrors);
        body.getError().setCode(OrderException.CODE_VALIDATION_FAILED);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Malformed request body"), HttpStatus.BAD_REQUEST);
    }

    // Never expose the raw message (may contain SQL/constraint details); log it instead.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        // Spring's own client/framework errors (missing header, 404, 405, 415, validation...) keep their status.
        if (e instanceof ErrorResponse errorResponse && !errorResponse.getStatusCode().is5xxServerError()) {
            HttpStatusCode status = errorResponse.getStatusCode();
            log.warn("Request error {}: {}", status, e.getMessage());
            String detail = errorResponse.getBody().getDetail();
            return new ResponseEntity<>(ApiResponse.error(detail != null ? detail : "Request error"), status);
        }
        log.error("Unexpected error: {}", e.getMessage(), e);
        return new ResponseEntity<>(ApiResponse.error("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
