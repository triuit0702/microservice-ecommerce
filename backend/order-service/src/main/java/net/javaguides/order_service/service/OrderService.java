package net.javaguides.order_service.service;


import net.javaguides.common_lib.dto.order.OrderDTO;
import net.javaguides.order_service.dto.OrderRequestDto;
import net.javaguides.order_service.entity.Order;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public interface OrderService {
    OrderDTO placeOrder(OrderRequestDto order, Long userId, String email, String idempotencyKey);

    /** Order of the given user; 404 if it does not exist or belongs to another user. */
    OrderDTO getOrderForUser(String orderId, Long userId);

    /** Orders of the given user, newest first. */
    Page<OrderDTO> getOrdersForUser(Long userId, int page, int size);

    Order getOrderById(String orderId);
    void saveOrder(Order order);

    /** Ids of orders still PENDING that were created before the given time, oldest first. */
    List<String> findExpiredPendingOrderIds(Date createdBefore, int limit);

    /**
     * Cancel the order if it is still PENDING and request a stock release, in one transaction.
     * @return true if the order was cancelled, false if it is no longer PENDING (or does not exist)
     */
    boolean cancelExpiredOrder(String orderId, String reason);

    /**
     * Write a RELEASE_STOCK event to the outbox, in the caller's transaction.
     * product-service gives the stock back only if it was reserved for this order, so it is safe to call more than once.
     */
    void requestStockRelease(Order order);
}
