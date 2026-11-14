package net.javaguides.common_lib.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderMessageEvent {
    /** {@link net.javaguides.common_lib.enums.OrderEventType} name; null is treated as CREATE_ORDER (older events). */
    private String eventType;

    private String orderId;
    private String status;
    private List<OrderItemDTO> orderItems;
    private Long userId;

    private String paymentMethod;
    private String email;

    /** Reason of an inventory CANCEL result, shown to the customer. */
    private String reason;
}
