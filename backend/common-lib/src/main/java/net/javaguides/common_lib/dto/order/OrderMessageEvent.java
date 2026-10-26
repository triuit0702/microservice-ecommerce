package net.javaguides.common_lib.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderMessageEvent {
    private String orderId;
    private String status;
    private List<OrderItemDTO> orderItems;
    private Long userId;

    private String paymentMethod;
    private String email;
}
