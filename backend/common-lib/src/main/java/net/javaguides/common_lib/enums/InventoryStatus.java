package net.javaguides.common_lib.enums;

/**
 * Stock reservation result sent from product-service to order-service.
 * RELEASED is internal to product-service: reserved stock was given back after the order was cancelled.
 */
public enum InventoryStatus {
    RESERVED,
    CANCEL,
    RELEASED
}
