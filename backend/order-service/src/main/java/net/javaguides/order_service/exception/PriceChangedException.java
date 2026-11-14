package net.javaguides.order_service.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Prices sent by the client no longer match the server prices.
 * Details map "productId:variantId" (or "shippingFee") to the current server price.
 */
public class PriceChangedException extends OrderException {
    private final Map<String, String> details;

    public PriceChangedException(String message, Map<String, String> details) {
        super(message, HttpStatus.CONFLICT, CODE_PRICE_CHANGED);
        this.details = details;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
