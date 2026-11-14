package net.javaguides.product_service.service;

import net.javaguides.common_lib.dto.order.OrderItemDTO;
import net.javaguides.product_service.entity.ProcessedOrder;

import java.util.List;
import java.util.Optional;

public interface InventoryService {

    /**
     * Get the processing record of an order.
     * @param orderId: order id
     * @return record if the order was processed before, otherwise empty
     */
    Optional<ProcessedOrder> findProcessedOrder(String orderId);

    /**
     * Atomically decrease stock for all items of an order and mark the order as RESERVED (not yet published).
     * All-or-nothing: if any item fails, the whole transaction is rolled back.
     * @throws net.javaguides.product_service.exception.InsufficientStockException variant missing or not enough stock
     * @throws net.javaguides.product_service.exception.ProductException invalid order items
     */
    void reserveStock(String orderId, List<OrderItemDTO> orderItems);

    /**
     * Mark the order as CANCEL (not yet published) without touching stock.
     */
    void markCancelled(String orderId);

    /**
     * Mark the order result as sent to order-service.
     */
    void markPublished(String orderId);

    /**
     * Give back the stock of a cancelled order, only if it was reserved and not released yet (idempotent).
     * @return true if stock was given back, false if there was nothing to release
     */
    boolean releaseStock(String orderId, List<OrderItemDTO> orderItems);
}
