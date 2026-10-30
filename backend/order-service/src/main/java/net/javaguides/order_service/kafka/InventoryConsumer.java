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
        // Handle the consumed message
        LOGGER.info("Received message from update-order-topic");
        Order order = orderService.getOrderById(orderMessageEvent.getOrderId());
        if (Objects.isNull(order)) {
            return;
        }
        // Idempotent: only a PENDING order can be updated; duplicated or late messages are ignored
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            LOGGER.info("Order {} already {}, skip inventory result {}",
                    orderMessageEvent.getOrderId(), order.getStatus(), orderMessageEvent.getStatus());
            return;
        }
        // Map inventory result to order status: RESERVED -> CONFIRMED, otherwise CANCELLED
        OrderStatus newStatus = InventoryStatus.RESERVED.name().equals(orderMessageEvent.getStatus())
                ? OrderStatus.CONFIRMED
                : OrderStatus.CANCELLED;
        order.setStatus(newStatus.name());
        LOGGER.info("Order {} status updated to {}", orderMessageEvent.getOrderId(), newStatus);
        orderService.saveOrder(order);    }
}
