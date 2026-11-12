package net.javaguides.order_service.service.impl;


import feign.FeignException;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.common_lib.dto.order.OrderDTO;
import net.javaguides.common_lib.dto.order.OrderItemDTO;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import net.javaguides.common_lib.enums.OrderStatus;
import net.javaguides.order_service.dto.OrderRequestDto;
import net.javaguides.order_service.dto.product.ProductResponseDto;
import net.javaguides.order_service.dto.product_variant.ProductVariantResponseDto;
import net.javaguides.order_service.entity.Order;
import net.javaguides.order_service.entity.ShippingInfo;
import net.javaguides.order_service.exception.OrderException;
import net.javaguides.order_service.kafka.OrderProducer;
import net.javaguides.order_service.kafka.OutboxWriter;
import net.javaguides.order_service.repository.OrderRepository;
import net.javaguides.order_service.service.OrderService;
import net.javaguides.order_service.service.ProductAPIClient;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderProducer orderProducer;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final ProductAPIClient productAPIClient;

    private final OutboxWriter outboxWriter;

    // Decided by the server, never taken from the request (frontend shows the same value)
    @Value("${order.shipping-fee:10}")
    private BigDecimal shippingFee;

//    @Override
//    @Transactional
//    public OrderDTO placeOrder(OrderRequestDto orderRequestDto, Long userId, String email) {
//        try {
//            OrderDTO newOrder = createOrderDTO(orderRequestDto, userId);
//            validateStockAndPrice(orderRequestDto.getOrderItems());
//
//            Order createdOrder = saveOrder(newOrder);
//            sendOrderEvent(createdOrder, orderRequestDto.getPaymentMethod(), email);
//
//            return modelMapper.map(createdOrder, OrderDTO.class);
//        } catch (OrderException e) {
//            throw e;
//        } catch (Exception e) {
//            LOGGER.error("Failed to create order: {}", e.getMessage(), e);
//            throw new OrderException("Failed to create order ", HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    /**
     * place order version 2: using outbox pattern to send event after transaction commit
     *
     * @param orderRequestDto The order request DTO containing order details.
     * @param userId          The ID of the user placing the order.
     * @param email           The email of the user placing the order.
     * @param idempotencyKey  Client key of this checkout attempt; a repeated key returns the existing order.
     * @return The created (or previously created) OrderDTO.
     */
    @Override
    @Transactional
    public OrderDTO placeOrder(OrderRequestDto orderRequestDto, Long userId, String email, String idempotencyKey) {
        // Fast path: same key already used (double click / retry after success) -> return the existing order
        Optional<Order> existingOrder = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingOrder.isPresent()) {
            LOGGER.info("Duplicate order request for user {} with idempotency key {}, returning order {}",
                    userId, idempotencyKey, existingOrder.get().getOrderId());
            return modelMapper.map(existingOrder.get(), OrderDTO.class);
        }

        try {
            List<ValidatedItem> items = validateStockAndPrice(orderRequestDto.getOrderItems());

            Order order = buildOrder(orderRequestDto, items, userId, idempotencyKey);
            // Flush now so a duplicate idempotency key fails here (before the outbox event is written)
            Order createdOrder = orderRepository.saveAndFlush(order);
           // sendOrderEvent(createdOrder, orderRequestDto.getPaymentMethod(), email);
            OrderMessageEvent orderMessageEvent = createOrderMessageEvent(createdOrder, orderRequestDto.getPaymentMethod(), email);

            // ghi event vào outbox, cùng transaction với đơn hàng
            outboxWriter.save(createdOrder.getOrderId(), orderMessageEvent);

            return modelMapper.map(createdOrder, OrderDTO.class);
        } catch (OrderException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to create order: {}", e.getMessage(), e);
            throw new OrderException("Failed to create order ", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private OrderMessageEvent createOrderMessageEvent(Order createdOrder, String paymentMethod, String email) {
        OrderMessageEvent orderMessageEvent = new OrderMessageEvent();
        orderMessageEvent.setOrderId(createdOrder.getOrderId());
        orderMessageEvent.setStatus(OrderStatus.PENDING.name());
        orderMessageEvent.setPaymentMethod(paymentMethod);
        orderMessageEvent.setEmail(email);
        orderMessageEvent.setUserId(createdOrder.getUserId());

        List<OrderItemDTO> orderItems = createdOrder.getOrderItems().stream()
                .map(item -> modelMapper.map(item, OrderItemDTO.class))
                .collect(Collectors.toList());
        orderMessageEvent.setOrderItems(orderItems);

        return orderMessageEvent;
    }

    /**
     * Get order by ID
     * @param orderId
     * @return
     */
    @Override
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @Override
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }


    /**
     * Build the order entity from the request and the validated items. Client-supplied
     * orderId/status/totals/prices are ignored; totals are computed by the entity.
     */
    private Order buildOrder(OrderRequestDto req, List<ValidatedItem> items, Long userId, String idempotencyKey) {
        ShippingInfo shippingInfo = new ShippingInfo(req.getAddress(), req.getPhone(), req.getReceiverName(), req.getEmail());
        Order order = Order.create(userId, idempotencyKey, shippingInfo, shippingFee);
        items.forEach(i -> order.addItem(i.productId(), i.variantId(), i.quantity(), i.unitPrice(), i.color()));
        return order;
    }

    /** Order line after validation, carrying the server-side unit price. */
    private record ValidatedItem(String productId, Long variantId, int quantity,
                                 BigDecimal unitPrice, String color) {}

    /**
     * Validates order items against product-service and returns them with server-side prices.
     * Stock check is fail-fast only; the real reservation happens later in product-service.
     */
    private List<ValidatedItem> validateStockAndPrice(List<OrderItemDTO> items) {
        validateItems(items);                                               // 1. input is well-formed
        Map<String, ProductResponseDto> productsById = loadProducts(items); // 2. all products exist
        checkStock(items, productsById);                                    // 3. enough stock per variant
        return applyServerPrices(items, productsById);                      // 4. never trust client price
    }

    private Map<String, ProductResponseDto> loadProducts(List<OrderItemDTO> items) {
        Set<String> ids = extractProductIds(items);
        Map<String, ProductResponseDto> productsById = fetchProducts(ids).stream()
                .collect(Collectors.toMap(ProductResponseDto::getId, Function.identity(), (a, b) -> a));

        Set<String> missingProductIds = new HashSet<>(ids);
        missingProductIds.removeAll(productsById.keySet());
        if (!missingProductIds.isEmpty()) {
            LOGGER.warn("Products not found: {}", missingProductIds);
            throw new OrderException("Some products are no longer available", HttpStatus.BAD_REQUEST);
        }
        return productsById;
    }

    /** Stock belongs to the variant, so lines of the same variant are summed before checking. */
    private void checkStock(List<OrderItemDTO> items, Map<String, ProductResponseDto> productsById) {
        Map<Long, Integer> requestedPerVariant = new HashMap<>();
        for (OrderItemDTO item : items) {
            ProductResponseDto product = productsById.get(item.getProductId());
            ProductVariantResponseDto variant = resolveVariant(product, item.getVariantId());
            int requested = requestedPerVariant.merge(variant.getId(), item.getQuantity(), Integer::sum);
            int available = Objects.requireNonNullElse(variant.getStockQuantity(), 0);
            if (available < requested) {
                LOGGER.info("Insufficient stock for variantId {}: requested {}, available {}",
                        variant.getId(), requested, available);
                throw new OrderException("Not enough stock for " + product.getName(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    private List<ValidatedItem> applyServerPrices(List<OrderItemDTO> items,
                                                  Map<String, ProductResponseDto> productsById) {
        return items.stream()
                .map(item -> {
                    ProductResponseDto product = productsById.get(item.getProductId());
                    ProductVariantResponseDto variant = resolveVariant(product, item.getVariantId());
                    return new ValidatedItem(item.getProductId(), item.getVariantId(), item.getQuantity(),
                            priceOf(product, variant), item.getColor());
                })
                .toList();
    }

    /** Variant price if set, otherwise the product price. */
    private BigDecimal priceOf(ProductResponseDto product, ProductVariantResponseDto variant) {
        BigDecimal price = variant.getPrice() != null ? variant.getPrice() : product.getPrice();
        if (price == null) {
            throw new OrderException("Price not available for " + product.getName(), HttpStatus.BAD_REQUEST);
        }
        return price;
    }

    private void validateItems(List<OrderItemDTO> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new OrderException("Order must contain at least one item", HttpStatus.BAD_REQUEST);
        }
        for (OrderItemDTO item : orderItems) {
            if (item == null || item.getProductId() == null) {
                throw new OrderException("Order item must have a productId", HttpStatus.BAD_REQUEST);
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderException("Item quantity must be greater than 0", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private Set<String> extractProductIds(List<OrderItemDTO> orderItems) {
        return orderItems.stream()
                .map(OrderItemDTO::getProductId)
                .collect(Collectors.toSet());
    }

    private List<ProductResponseDto> fetchProducts(Set<String> productIds) {
        try {
            ApiResponse<List<ProductResponseDto>> body = productAPIClient.getProductsByIds(productIds).getBody();
            if (body == null || body.getData() == null) {
                throw new OrderException("Product service returned no data", HttpStatus.BAD_GATEWAY);
            }
            return body.getData();
        } catch (FeignException e) {
            LOGGER.error("Product service call failed, status {}: {}", e.status(), e.getMessage());
            throw new OrderException("Product service is unavailable, please try again later",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Resolves the ordered variant. Stock is tracked per variant, so a product
     * without variants cannot be ordered and a valid variantId is always required.
     */
    private ProductVariantResponseDto resolveVariant(ProductResponseDto product, Long variantId) {
        List<ProductVariantResponseDto> variants = product.getVariants();
        if (variants == null || variants.isEmpty()) {
            throw new OrderException(product.getName() + " is not available for sale", HttpStatus.BAD_REQUEST);
        }
        if (variantId == null) {
            throw new OrderException("Variant is required for " + product.getName(), HttpStatus.BAD_REQUEST);
        }
        return variants.stream()
                .filter(v -> variantId.equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> new OrderException(
                        "Variant " + variantId + " not found for " + product.getName(), HttpStatus.BAD_REQUEST));
    }

//    private void sendOrderEvent(Order createdOrder, String paymentMethod, String email) {
//        OrderEvent orderEvent = createOrderEvent(createdOrder, paymentMethod, email);
//        orderProducer.sendMessage(orderEvent);
//    }


}
