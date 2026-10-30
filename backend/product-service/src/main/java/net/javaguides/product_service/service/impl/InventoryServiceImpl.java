package net.javaguides.product_service.service.impl;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderItemDTO;
import net.javaguides.common_lib.enums.InventoryStatus;
import net.javaguides.product_service.entity.ProcessedOrder;
import net.javaguides.product_service.exception.InsufficientStockException;
import net.javaguides.product_service.exception.ProductException;
import net.javaguides.product_service.repository.ProcessedOrderRepository;
import net.javaguides.product_service.repository.ProductVariantRepository;
import net.javaguides.product_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductVariantRepository productVariantRepository;
    private final ProcessedOrderRepository processedOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedOrder> findProcessedOrder(String orderId) {
        return processedOrderRepository.findById(orderId);
    }

    @Override
    @Transactional
    public void reserveStock(String orderId, List<OrderItemDTO> orderItems) {
        // Insert first: a duplicate orderId fails fast before any stock row is locked
        processedOrderRepository.saveAndFlush(new ProcessedOrder(orderId, InventoryStatus.RESERVED, false, LocalDateTime.now()));

        // Sorted by variantId so concurrent orders lock rows in the same order (avoid deadlocks)
        for (Map.Entry<Long, Integer> entry : mergeQuantities(orderItems).entrySet()) {
            Long variantId = entry.getKey();
            int quantity = entry.getValue();

            int updated = productVariantRepository.decreaseStock(variantId, quantity);
            if (updated == 0) {
                LOGGER.warn("Order {}: variant {} not found or insufficient stock (required {})", orderId, variantId, quantity);
                throw new InsufficientStockException("Variant " + variantId + " not found or insufficient stock");
            }
        }
    }

    @Override
    @Transactional
    public void markCancelled(String orderId) {
        processedOrderRepository.saveAndFlush(new ProcessedOrder(orderId, InventoryStatus.CANCEL, false, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void markPublished(String orderId) {
        processedOrderRepository.markPublished(orderId);
    }

    /**
     * Validate items and sum quantities of duplicated variants.
     */
    private Map<Long, Integer> mergeQuantities(List<OrderItemDTO> orderItems) {
        if (CollectionUtils.isEmpty(orderItems)) {
            throw new ProductException("Order has no items");
        }

        Map<Long, Integer> quantityByVariant = new TreeMap<>();
        for (OrderItemDTO item : orderItems) {
            if (item.getVariantId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ProductException("Invalid order item: " + item);
            }
            quantityByVariant.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
        }
        return quantityByVariant;
    }
}
