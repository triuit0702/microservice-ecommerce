package net.javaguides.common_lib.enums;

/**
 * Type of an event sent from order-service to product-service on the create-order topic.
 * Both types use the orderId as Kafka key, so events of one order are consumed in order.
 */
public enum OrderEventType {
    /** New order: reserve (deduct) stock and reply RESERVED / CANCEL. */
    CREATE_ORDER,
    /** Order cancelled after stock may have been reserved: give the stock back. No reply. */
    RELEASE_STOCK
}
