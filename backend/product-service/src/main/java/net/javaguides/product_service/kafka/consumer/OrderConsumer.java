package net.javaguides.product_service.kafka.consumer;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import net.javaguides.common_lib.enums.InventoryStatus;
import net.javaguides.product_service.entity.ProcessedOrder;
import net.javaguides.product_service.exception.InsufficientStockException;
import net.javaguides.product_service.exception.ProductException;
import net.javaguides.product_service.kafka.producer.InventoryProducer;
import net.javaguides.product_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumer.class);

    private final InventoryService inventoryService;
    private final InventoryProducer inventoryProducer;

    /**
     * Reserve stock for a new order and reply RESERVED / CANCEL to order-service.
     * Not transactional on purpose: the stock transaction commits inside InventoryService
     * before the result is published, so order-service never sees RESERVED for a rolled-back reservation.
     */
    @KafkaListener(topics = "${spring.kafka.create-order-topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderMessageEvent orderEvent) {
        LOGGER.info("Received OrderEvent: {}", orderEvent);

        String orderId = orderEvent.getOrderId();
        if (orderId == null) {
            LOGGER.error("Skip OrderEvent without orderId: {}", orderEvent);
            return;
        }

        // Redelivered message: never touch stock again
        Optional<ProcessedOrder> processedOrder = inventoryService.findProcessedOrder(orderId);
        if (processedOrder.isPresent()) {
            if (processedOrder.get().isPublished()) {
                LOGGER.info("Duplicate OrderEvent {}, result already sent, skip", orderId);
                return;
            }
            // Stock was handled but the previous send failed: send the stored result
            LOGGER.info("Order {} already processed with status {}, sending pending result", orderId, processedOrder.get().getStatus());
            publishResult(orderId, processedOrder.get().getStatus());
            return;
        }

        reserveStock(orderEvent).ifPresent(status -> publishResult(orderId, status));
    }

    /**
     * Try to reserve stock; business failures become CANCEL, technical failures are rethrown for Kafka retry.
     * @return result to publish, or empty if another consumer is handling the same order
     */
    private Optional<InventoryStatus> reserveStock(OrderMessageEvent orderEvent) {
        String orderId = orderEvent.getOrderId();
        try {
            inventoryService.reserveStock(orderId, orderEvent.getOrderItems());
            LOGGER.info("Order {} reserved", orderId);
            return Optional.of(InventoryStatus.RESERVED);
        } catch (InsufficientStockException | ProductException e) {
            // Reservation transaction is fully rolled back here, no partial stock deduction
            LOGGER.warn("Order {} cancelled: {}", orderId, e.getMessage());
            inventoryService.markCancelled(orderId);
            return Optional.of(InventoryStatus.CANCEL);
        } catch (DataIntegrityViolationException e) {
            // Another consumer processed the same order concurrently; it publishes the result itself
            LOGGER.info("Order {} is being processed by another consumer, skip", orderId);
            return Optional.empty();
        }
    }

    /**
     * Send the result to order-service, then flag it as published.
     * If sending fails, the exception triggers a Kafka redelivery and the result is sent again.
     */
    private void publishResult(String orderId, InventoryStatus status) {
        OrderMessageEvent updatedOrderEvent = new OrderMessageEvent();
        updatedOrderEvent.setOrderId(orderId);
        updatedOrderEvent.setStatus(status.name());

        inventoryProducer.sendOrderStatusUpdate(updatedOrderEvent);
        inventoryService.markPublished(orderId);
    }
}
