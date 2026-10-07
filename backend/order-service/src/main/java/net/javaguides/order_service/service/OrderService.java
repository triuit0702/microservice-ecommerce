package net.javaguides.order_service.service;


import net.javaguides.common_lib.dto.order.OrderDTO;
import net.javaguides.order_service.dto.OrderRequestDto;
import net.javaguides.order_service.entity.Order;

public interface OrderService {
    OrderDTO placeOrder(OrderRequestDto order, Long userId, String email);
    Order getOrderById(String orderId);
    void saveOrder(Order order);
}
