package net.javaguides.order_service.service;


import net.javaguides.common_lib.dto.order.OrderDTO;
import net.javaguides.order_service.dto.OrderRequestDto;
import net.javaguides.order_service.entity.Order;
import org.springframework.data.domain.Page;

public interface OrderService {
    OrderDTO placeOrder(OrderRequestDto order, Long userId, String email, String idempotencyKey);

    /** Order of the given user; 404 if it does not exist or belongs to another user. */
    OrderDTO getOrderForUser(String orderId, Long userId);

    /** Orders of the given user, newest first. */
    Page<OrderDTO> getOrdersForUser(Long userId, int page, int size);

    Order getOrderById(String orderId);
    void saveOrder(Order order);
}
