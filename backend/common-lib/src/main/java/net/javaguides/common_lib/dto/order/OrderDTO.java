package net.javaguides.common_lib.dto.order;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private String orderId;
    private String status;
    private List<OrderItemDTO> orderItems;
    private Long userId;
    private Date createdAt;
    private Date updatedAt;
    private int version;

    // Totals computed by order-service
    private BigDecimal subTotal;
    private BigDecimal shippingFee;
    private BigDecimal totalPrice;

    // Shipping info
    private String address;
    private String phone;
    private String receiverName;
    private String email;

    private String paymentMethod;

    /** Why the order was cancelled (only set when status is CANCELLED). */
    private String cancelReason;
}
