package net.javaguides.product_service.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_VARIANTS_REQUIRED(HttpStatus.BAD_REQUEST),
    INVALID_VARIANT_DATA(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
