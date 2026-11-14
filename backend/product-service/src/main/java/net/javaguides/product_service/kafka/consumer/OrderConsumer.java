package net.javaguides.product_service.kafka.consumer;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import net.javaguides.common_lib.enums.InventoryStatus;
import net.javaguides.product_service.entity.ProcessedOrder;
import net.javaguides.product_service.exception.InsufficientStockException;
import net.javaguides.product_service.exception.ProductException;
import net.javaguides.product_service.kafka.producer.InventoryProducer;
import net.javaguides.product_service.service.CartService;
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
    private static final String REASON_OUT_OF_STOCK = "Some items are out of stock";
    private static final String REASON_INVALID_ITEMS = "Some items are no longer available";

    private final InventoryService inventoryService;
    private final InventoryProducer inventoryProducer;
    private final CartService cartService;

    /** Reservation result and the reason sent to the customer when cancelled. */
    private record ReserveResult(InventoryStatus status, String reason) {}

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
            // (reason is not stored; order-service falls back to a default reason)
            InventoryStatus status = processedOrder.get().getStatus();
            LOGGER.info("Order {} already processed with status {}, sending pending result", orderId, status);
            publishResult(orderEvent, new ReserveResult(status, null));
            return;
        }

        reserveStock(orderEvent).ifPresent(result -> publishResult(orderEvent, result));
    }

    /**
     * Try to reserve stock; business failures become CANCEL, technical failures are rethrown for Kafka retry.
     * @return result to publish, or empty if another consumer is handling the same order
     */
    private Optional<ReserveResult> reserveStock(OrderMessageEvent orderEvent) {
        String orderId = orderEvent.getOrderId();
        try {
            inventoryService.reserveStock(orderId, orderEvent.getOrderItems());
            LOGGER.info("Order {} reserved", orderId);
            return Optional.of(new ReserveResult(InventoryStatus.RESERVED, null));
        } catch (InsufficientStockException | ProductException e) {
            // Reservation transaction is fully rolled back here, no partial stock deduction
            LOGGER.warn("Order {} cancelled: {}", orderId, e.getMessage());
            inventoryService.markCancelled(orderId);
            String reason = e instanceof InsufficientStockException ? REASON_OUT_OF_STOCK : REASON_INVALID_ITEMS;
            return Optional.of(new ReserveResult(InventoryStatus.CANCEL, reason));
        } catch (DataIntegrityViolationException e) {
            // Another consumer processed the same order concurrently; it publishes the result itself
            LOGGER.info("Order {} is being processed by another consumer, skip", orderId);
            return Optional.empty();
        }
    }

    /**
     * Send the result to order-service, then flag it as published.
     * If sending fails, the exception triggers a Kafka redelivery and the result is sent again.
     * On RESERVED the ordered items are also removed from the customer's cart.
     */
    private void publishResult(OrderMessageEvent orderEvent, ReserveResult result) {
        String orderId = orderEvent.getOrderId();
        OrderMessageEvent updatedOrderEvent = new OrderMessageEvent();
        updatedOrderEvent.setOrderId(orderId);
        updatedOrderEvent.setStatus(result.status().name());
        updatedOrderEvent.setReason(result.reason());

        inventoryProducer.sendOrderStatusUpdate(updatedOrderEvent);
        inventoryService.markPublished(orderId);

        if (result.status() == InventoryStatus.RESERVED) {
            clearOrderedItemsFromCart(orderEvent);
        }
    }

    /**
     * Best effort: a Redis failure must not redeliver the order (stock is already reserved),
     * so it is only logged; the customer can still remove the items manually.
     */
    private void clearOrderedItemsFromCart(OrderMessageEvent orderEvent) {
        try {
            cartService.removeOrderedItems(orderEvent.getUserId(), orderEvent.getOrderItems());
            LOGGER.info("Order {}: ordered items removed from cart of user {}", orderEvent.getOrderId(), orderEvent.getUserId());
        } catch (Exception e) {
            LOGGER.warn("Order {}: failed to remove ordered items from cart of user {}: {}",
                    orderEvent.getOrderId(), orderEvent.getUserId(), e.getMessage());
        }
    }
}
