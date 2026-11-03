package net.javaguides.product_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the caller is not authenticated or tries to access another user's cart.
 */
@Getter
public class CartAccessException extends RuntimeException {

    private final HttpStatus status;

    public CartAccessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
