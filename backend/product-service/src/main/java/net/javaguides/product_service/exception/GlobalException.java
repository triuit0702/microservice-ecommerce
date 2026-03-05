package net.javaguides.product_service.exception;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import net.javaguides.common_lib.dto.ApiResponse;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@ControllerAdvice
public class GlobalException {

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<String>> handleMultipartException(MultipartException e) {
        return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("ResourceNotFoundException: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBindException(BindException ex) {

        log.error("BindException exception: {}", ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

         return ResponseEntity.badRequest()
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {

        log.error("Business exception: {}", ex.getMessage(), ex);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<?> handleOptimisticLockException(OptimisticLockException ex) {

        log.error("Optimistic Lock Exception : {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<?> handleProductException(ProductException ex) {

        log.error("Product Exception : {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {

        log.warn("Optimistic locking failure: entity={}, id={}",
                ex.getPersistentClass().getSimpleName(),
                ex.getIdentifier()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Version conflict!");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        log.error("Unhandled exception occurred", ex); // 🔥 in full stacktrace
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
