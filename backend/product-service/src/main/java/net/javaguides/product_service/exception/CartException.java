package net.javaguides.product_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Business error for cart operations (invalid quantity, not enough stock, cart full...).
 */
@Getter
public class CartException extends RuntimeException {

    private final HttpStatus status;

    public CartException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
