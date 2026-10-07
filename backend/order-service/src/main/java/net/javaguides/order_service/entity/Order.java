package net.javaguides.order_service.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.javaguides.common_lib.entity.AbstractEntity;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends AbstractEntity {
    @Id
    private String orderId;

    private String status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    private Long userId;

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


