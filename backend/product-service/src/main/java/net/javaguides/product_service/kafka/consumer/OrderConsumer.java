package net.javaguides.product_service.kafka.consumer;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderDTO;
import net.javaguides.common_lib.dto.order.OrderEvent;
import net.javaguides.common_lib.dto.order.OrderItemDTO;
import net.javaguides.product_service.entity.ProductVariant;
import net.javaguides.product_service.exception.InsufficientStockException;
import net.javaguides.product_service.kafka.producer.InventoryProducer;
import net.javaguides.product_service.service.ProductVariantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumer.class);

    private final ProductVariantService productVariantService;
    private final InventoryProducer inventoryProducer;

    @KafkaListener(topics = "${spring.kafka.create-order-topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(OrderEvent orderEvent) {
        LOGGER.info("Received OrderEvent: {}", orderEvent);

        OrderDTO orderDTO = orderEvent.getOrderDTO();
        Set<Long> variantIds = orderDTO.getOrderItems().stream()
                .map(OrderItemDTO::getVariantId)
                .collect(Collectors.toSet());

        // get list product variant by variantIds
        List<ProductVariant> variants = Optional.ofNullable(
                productVariantService.getProductVariantByIds(variantIds)
        ).orElse(Collections.emptyList());

        if (CollectionUtils.isEmpty(variants)) {
            return;
        }

        // create map variantId -> ProductVariant for easy access
        Map<Long, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, variant -> variant));

        // update stock
        updateStock(orderDTO, variantMap);
    }

    /**
     * Update the stock quantity of product variants based on the order items.
     *
     * @param orderDTO   The order data transfer object containing order items.
     * @param variantMap A map of variant IDs to ProductVariant objects for easy access.
     */
    private void updateStock(OrderDTO orderDTO, Map<Long, ProductVariant> variantMap) {

        List<ProductVariant> productVariantList = new ArrayList<>();
        for (OrderItemDTO orderItem : orderDTO.getOrderItems()) {

            Long variantId = orderItem.getVariantId();
            ProductVariant productVariant = variantMap.get(variantId);

            if (productVariant == null) {
                LOGGER.error("ProductVariant with id {} not found.", variantId);
                throw new RuntimeException("ProductVariant with id " + variantId + " not found.");
            }

            int requiredQuantity = orderItem.getQuantity();
            int currentStock = productVariant.getStockQuantity();
            int updatedStock = currentStock - requiredQuantity;

            if (updatedStock < 0) {
                LOGGER.error("Insufficient stock for variant {}. Required: {}, Available: {}", variantId, requiredQuantity, currentStock);
                sendOrderStatusUpdate(orderDTO,"CANCEL");
                throw new InsufficientStockException("Insufficient stock for variant " + variantId);
            }

            productVariant.setStockQuantity(updatedStock);
            productVariantList.add(productVariant);
        }

        productVariantService.saveAllListVariant(productVariantList);

        // push event to kafka for order status update
        sendOrderStatusUpdate(orderDTO,"RESERVED");

    }

    /**
     * Send an order status update to the Kafka topic.
     *
     * @param orderDTO The order data transfer object containing order details.
     * @param status   The new status of the order (e.g., "RESERVED", "CANCEL").
     */
    private void sendOrderStatusUpdate(OrderDTO orderDTO, String status) {
        OrderDTO updatedOrderDTO = new OrderDTO();
        updatedOrderDTO.setOrderId(orderDTO.getOrderId());
        updatedOrderDTO.setStatus(status);

        inventoryProducer.sendOrderStatusUpdate(updatedOrderDTO);
    }
}
