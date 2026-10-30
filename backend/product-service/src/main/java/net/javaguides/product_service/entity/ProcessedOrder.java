package net.javaguides.product_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.javaguides.common_lib.enums.InventoryStatus;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

/**
 * Records orders whose stock reservation has already been handled,
 * so redelivered Kafka messages do not deduct stock twice.
 */
@Entity
@Table(name = "processed_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedOrder implements Persistable<String> {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private String orderId;

    /** Reservation result sent back to order-service. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status;

    /** True once the result has been sent to order-service; redelivered messages are then skipped. */
    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Override
    public String getId() {
        return orderId;
    }

    /** Always insert (never merge), so a duplicate orderId fails with a constraint violation. */
    @Override
    public boolean isNew() {
        return true;
    }
}
