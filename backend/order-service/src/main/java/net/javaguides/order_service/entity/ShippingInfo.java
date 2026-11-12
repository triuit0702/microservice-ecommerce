package net.javaguides.order_service.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Delivery information of an order. Embedded in {@link Order}; columns keep their original names
 * (address, phone, receiver_name, email).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ShippingInfo {
    private String address;
    private String phone;
    private String receiverName;
    private String email;
}
