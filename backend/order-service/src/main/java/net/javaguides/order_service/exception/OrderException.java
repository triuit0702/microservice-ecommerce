package net.javaguides.order_service.exception;

import org.springframework.http.HttpStatus;

public class OrderException extends RuntimeException {
    /** Codes returned in ApiError.code so the frontend can tell errors with the same HTTP status apart. */
    public static final String CODE_PRICE_CHANGED = "PRICE_CHANGED";
    public static final String CODE_ORDER_IN_PROGRESS = "ORDER_IN_PROGRESS";
    public static final String CODE_VALIDATION_FAILED = "VALIDATION_FAILED";

    private final HttpStatus status;
    private final String code;

    public OrderException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public OrderException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
