package net.javaguides.order_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.javaguides.common_lib.entity.AbstractEntity;
import net.javaguides.common_lib.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate. Created only through {@link #create}; items are added with {@link #addItem}
 * so the item/order relation and the totals always stay consistent.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_user_idempotency_key",
                columnNames = {"user_id", "idempotency_key"}))
public class Order extends AbstractEntity {
    @Id
    private String orderId;

    /** Client-generated key per checkout attempt; unique per user so a retried/double-clicked request cannot create a second order. */
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    private String status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Long userId;

    // tổng tiền hàng
    private BigDecimal subTotal;

    // phí ship
    private BigDecimal shippingFee;

    // tổng thanh toán
    private BigDecimal totalPrice;

    // thông tin giao hàng: address, phone, receiver_name, email
    @Embedded
    private ShippingInfo shippingInfo;

    private String paymentMethod;

    /** Set when the order is cancelled, shown to the customer. */
    private String cancelReason;

    /**
     * Create a new PENDING order without items.
     */
    public static Order create(Long userId, String idempotencyKey, ShippingInfo shippingInfo,
                               BigDecimal shippingFee, String paymentMethod) {
        Order order = new Order();
        order.orderId = UUID.randomUUID().toString();
        order.status = OrderStatus.PENDING.name();
        order.userId = userId;
        order.idempotencyKey = idempotencyKey;
        order.shippingInfo = shippingInfo;
        order.shippingFee = shippingFee;
        order.paymentMethod = paymentMethod;
        order.recalculateTotals();
        return order;
    }

    /**
     * Add an item and keep both sides of the relation and the totals in sync.
     */
    public void addItem(String productId, Long variantId, Integer quantity, BigDecimal price, String color) {
        orderItems.add(OrderItem.of(this, productId, variantId, quantity, price, color));
        recalculateTotals();
    }

    /** Inventory reserved by product-service. */
    public void confirm() {
        this.status = OrderStatus.CONFIRMED.name();
    }

    /** Inventory could not be reserved. */
    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED.name();
        this.cancelReason = reason;
    }

    private void recalculateTotals() {
        this.subTotal = orderItems.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalPrice = subTotal.add(shippingFee);
    }
}
