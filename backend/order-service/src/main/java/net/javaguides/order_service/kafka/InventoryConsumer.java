package net.javaguides.order_service.kafka;


import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import net.javaguides.common_lib.enums.InventoryStatus;
import net.javaguides.common_lib.enums.OrderStatus;
import net.javaguides.order_service.entity.Order;
import net.javaguides.order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InventoryConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryConsumer.class);
    private static final String DEFAULT_CANCEL_REASON = "Some items are out of stock";
    private final OrderService orderService;

    /**
     * Consumes messages from the Kafka topic specified in the application properties.
     * Updates the order status based on the consumed message.
     *
     * @param orderMessageEvent The order message event containing order details.
     */
    @KafkaListener(topics = "${spring.kafka.update-order-topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(OrderMessageEvent orderMessageEvent) {
        String orderId = orderMessageEvent.getOrderId();
        String result = orderMessageEvent.getStatus();
        LOGGER.info("Received inventory result {} for order {}", result, orderId);

        // Only the two known results change an order; anything else (null, typo, new value) is ignored
        boolean reserved = InventoryStatus.RESERVED.name().equals(result);
        boolean cancelled = InventoryStatus.CANCEL.name().equals(result);
        if (orderId == null || (!reserved && !cancelled)) {
            LOGGER.warn("Skip invalid inventory result: orderId={}, status={}", orderId, result);
            return;
        }

        Order order = orderService.getOrderById(orderId);
        if (Objects.isNull(order)) {
            LOGGER.warn("Order {} not found, skip inventory result {}", orderId, result);
            return;
        }
        // Idempotent: only a PENDING order can be updated; duplicated or late messages are ignored
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            LOGGER.info("Order {} already {}, skip inventory result {}", orderId, order.getStatus(), result);
            return;
        }
        if (reserved) {
            order.confirm();
        } else {
            String reason = orderMessageEvent.getReason();
            order.cancel(reason == null || reason.isBlank() ? DEFAULT_CANCEL_REASON : reason);
        }
        LOGGER.info("Order {} status updated to {}", orderId, order.getStatus());
        orderService.saveOrder(order);
    }
}
