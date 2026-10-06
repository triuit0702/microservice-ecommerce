package net.javaguides.product_service.exception;

import org.springframework.http.HttpStatus;

public class ProductException extends  RuntimeException {

    public ProductException(String message){
        super(message);
    }

    public ProductException(String message,  Throwable cause){
        super(message, cause);
    }

}
