package net.javaguides.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderItemDTO;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {
    private String orderId;
    private String status;
    private List<OrderItemDTO> orderItems;
    private String paymentMethod;

    // tổng tiền hàng
    private BigDecimal subTotal;

    // phí ship
    private BigDecimal shippingFee;

    // tổng thanh toán
    private BigDecimal totalPrice;

    // thông tin user đặt hàng
    // địa chỉ
    private String address;
    // số điện thoại
    private String phone;
    // tên người nhận
    private String receiverName;
    // email người nhận
    private String email;
}
