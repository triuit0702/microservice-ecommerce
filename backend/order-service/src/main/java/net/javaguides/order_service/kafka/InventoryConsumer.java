package net.javaguides.order_service.kafka;


import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderDTO;
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
     * @param orderDTO The order data transfer object containing order details.
     */
    @KafkaListener(topics = "${spring.kafka.update-order-topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(OrderDTO orderDTO) {
        // Handle the consumed message
        LOGGER.info("Received message from update-order-topic");
        Order order = orderService.getOrderById(orderDTO.getOrderId());
        if (Objects.isNull(order)) {
            return;
        }
        order.setStatus("CANCEL");
        if (orderDTO.getStatus().equals("RESERVED")) {
            // Update order status to RESERVED in the database
            LOGGER.info("Order status updated to RESERVED for orderId: " + orderDTO.getOrderId());
            order.setStatus("CONFIRMED");
        }
        orderService.saveOrder(order);
    }
}
