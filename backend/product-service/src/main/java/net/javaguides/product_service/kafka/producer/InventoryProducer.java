package net.javaguides.product_service.kafka.producer;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class InventoryProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryProducer.class);

    private final KafkaTemplate<String, OrderMessageEvent> kafkaTemplate;

    @Value("${spring.kafka.update-order-topic.name}")
    private String updateOrderTopic;

    /**
     * Publish the reservation result, keyed by orderId to keep per-order ordering.
     * Blocks until the broker acks so a failure propagates to the listener and triggers a retry.
     */
    public void sendOrderStatusUpdate(OrderMessageEvent orderMessageEvent) {
        try {
            kafkaTemplate.send(updateOrderTopic, orderMessageEvent.getOrderId(), orderMessageEvent).get();
            LOGGER.info("Sent order status {} for order {}", orderMessageEvent.getStatus(), orderMessageEvent.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending order status update", e);
        } catch (Exception e) {
            LOGGER.error("Failed to send order status update for order {}", orderMessageEvent.getOrderId(), e);
            throw new IllegalStateException("Failed to send order status update", e);
        }
    }
}
