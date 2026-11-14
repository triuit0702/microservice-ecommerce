package net.javaguides.order_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderItemDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Place-order request. Shipping fields are validated here (@Valid in the controller);
 * order items are validated in OrderServiceImpl because OrderItemDTO lives in common-lib.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {
    // Ignored by the server (generated / computed there)
    private String orderId;
    private String status;

    private List<OrderItemDTO> orderItems;

    // Only cash on delivery is supported for now
    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "COD", message = "Unsupported payment method")
    private String paymentMethod;

    // tổng tiền hàng (ignored, computed by the server)
    private BigDecimal subTotal;

    // phí ship: compared with the server value, 409 if different
    private BigDecimal shippingFee;

    // tổng thanh toán (ignored, computed by the server)
    private BigDecimal totalPrice;

    // thông tin user đặt hàng
    // địa chỉ
    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    // số điện thoại: 8-15 digits, optional leading +
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone must be 8-15 digits")
    private String phone;

    // tên người nhận
    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must be at most 100 characters")
    private String receiverName;

    // email người nhận
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;
}
