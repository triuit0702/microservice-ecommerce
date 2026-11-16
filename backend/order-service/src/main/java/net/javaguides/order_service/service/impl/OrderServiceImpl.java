package net.javaguides.order_service.service.impl;


import feign.FeignException;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.common_lib.dto.order.OrderDTO;
import net.javaguides.common_lib.dto.order.OrderItemDTO;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import net.javaguides.common_lib.enums.OrderEventType;
import net.javaguides.common_lib.enums.OrderStatus;
import net.javaguides.order_service.dto.OrderRequestDto;
import net.javaguides.order_service.dto.product.ProductResponseDto;
import net.javaguides.order_service.dto.product_variant.ProductVariantResponseDto;
import net.javaguides.order_service.entity.Order;
import net.javaguides.order_service.entity.ShippingInfo;
import net.javaguides.order_service.exception.OrderException;
import net.javaguides.order_service.exception.PriceChangedException;
import net.javaguides.order_service.kafka.OrderProducer;
import net.javaguides.order_service.kafka.OutboxWriter;
import net.javaguides.order_service.repository.OrderRepository;
import net.javaguides.order_service.service.OrderService;
import net.javaguides.order_service.service.ProductAPIClient;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final int MAX_PAGE_SIZE = 50;
    // Input limits: keep quantities far from int overflow and the by-ids query URL short
    private static final int MAX_ORDER_ITEMS = 50;
    private static final int MAX_QUANTITY_PER_ITEM = 100;
    private static final String SHIPPING_FEE_KEY = "shippingFee";

    private final OrderProducer orderProducer;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final ProductAPIClient productAPIClient;

    private final OutboxWriter outboxWriter;
    // Programmatic transactions: placeOrder must not hold a DB connection during the Feign call
    private final TransactionTemplate transactionTemplate;

    // Decided by the server; the value sent by the client is only compared against it
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
     * place order version 2: using outbox pattern to send event after transaction commit.
     * Not @Transactional on purpose: validation calls product-service (Feign) without holding a DB
     * connection, and only the insert of the order + outbox event runs in a short transaction.
     *
     * @param orderRequestDto The order request DTO containing order details.
     * @param userId          The ID of the user placing the order.
     * @param email           The email of the user placing the order.
     * @param idempotencyKey  Client key of this checkout attempt; a repeated key returns the existing order.
     * @return The created (or previously created) OrderDTO.
     */
    @Override
    public OrderDTO placeOrder(OrderRequestDto orderRequestDto, Long userId, String email, String idempotencyKey) {
        // 1. Fast path: same key already used (double click / retry after success) -> return the existing order
        Optional<OrderDTO> existingOrder = findOrderByIdempotencyKey(userId, idempotencyKey);
        if (existingOrder.isPresent()) {
            LOGGER.info("Duplicate order request for user {} with idempotency key {}, returning order {}",
                    userId, idempotencyKey, existingOrder.get().getOrderId());
            return existingOrder.get();
        }

        try {
            // 2. Validation against product-service, outside any transaction
            List<ValidatedItem> items = validateStockAndPrice(orderRequestDto);

            // 3. Short transaction: order and outbox event are committed together
            return transactionTemplate.execute(status ->
                    createOrder(orderRequestDto, items, userId, email, idempotencyKey));
        } catch (OrderException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            // 4. A concurrent request with the same key committed first (InnoDB makes this insert wait
            //    for it), so the order is visible to a new transaction. Not found -> another integrity error.
            return findOrderByIdempotencyKey(userId, idempotencyKey)
                    .map(order -> {
                        LOGGER.info("Concurrent order request for user {} with idempotency key {}, returning order {}",
                                userId, idempotencyKey, order.getOrderId());
                        return order;
                    })
                    .orElseThrow(() -> {
                        LOGGER.error("Failed to create order: {}", e.getMessage(), e);
                        return new OrderException("Failed to create order", HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        } catch (PessimisticLockingFailureException e) {
            // Lock wait timeout / deadlock with a concurrent insert: retrying with the same key is safe
            LOGGER.warn("Lock conflict while creating order for user {} with idempotency key {}: {}",
                    userId, idempotencyKey, e.getMessage());
            throw new OrderException("Your order is being processed, please try again",
                    HttpStatus.CONFLICT, OrderException.CODE_ORDER_IN_PROGRESS);
        } catch (Exception e) {
            LOGGER.error("Failed to create order: {}", e.getMessage(), e);
            throw new OrderException("Failed to create order", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Runs inside the transaction of placeOrder step 3. */
    private OrderDTO createOrder(OrderRequestDto req, List<ValidatedItem> items, Long userId,
                                 String email, String idempotencyKey) {
        Order order = buildOrder(req, items, userId, idempotencyKey);
        // Flush now so a duplicate idempotency key fails here (before the outbox event is written)
        Order createdOrder = orderRepository.saveAndFlush(order);
        OrderMessageEvent orderMessageEvent = createOrderMessageEvent(createdOrder, req.getPaymentMethod(), email);

        // ghi event vào outbox, cùng transaction với đơn hàng
        outboxWriter.save(createdOrder.getOrderId(), orderMessageEvent);

        return toOrderDTO(createdOrder);
    }

    /** Reads the order of this checkout attempt in its own transaction (fresh snapshot, lazy items loaded). */
    private Optional<OrderDTO> findOrderByIdempotencyKey(Long userId, String idempotencyKey) {
        return transactionTemplate.execute(status ->
                orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(this::toOrderDTO));
    }

    private OrderMessageEvent createOrderMessageEvent(Order createdOrder, String paymentMethod, String email) {
        OrderMessageEvent orderMessageEvent = new OrderMessageEvent();
        orderMessageEvent.setEventType(OrderEventType.CREATE_ORDER.name());
        orderMessageEvent.setOrderId(createdOrder.getOrderId());
        orderMessageEvent.setStatus(OrderStatus.PENDING.name());
        orderMessageEvent.setPaymentMethod(paymentMethod);
        orderMessageEvent.setEmail(email);
        orderMessageEvent.setUserId(createdOrder.getUserId());
        orderMessageEvent.setOrderItems(toOrderItemDTOs(createdOrder));

        return orderMessageEvent;
    }

    private List<OrderItemDTO> toOrderItemDTOs(Order order) {
        return order.getOrderItems().stream()
                .map(item -> modelMapper.map(item, OrderItemDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean cancelExpiredOrder(String orderId, String reason) {
        Order order = orderRepository.findById(orderId).orElse(null);
        // Re-checked inside the transaction: the inventory result may have arrived since the order was selected
        if (order == null || !OrderStatus.PENDING.name().equals(order.getStatus())) {
            LOGGER.warn("Skip timeout cancel for order {}: status is {}",
                    orderId, order == null ? "<not found>" : order.getStatus());
            return false;
        }
        order.cancel(reason);
        // Stock may already be reserved (result late or stuck in the DLT): ask product-service to give it back.
        // If nothing was reserved, product-service ignores it.
        requestStockRelease(order);
        LOGGER.info("Order {} cancelled: still PENDING after timeout", orderId);
        return true;
    }

    @Override
    public void requestStockRelease(Order order) {
        OrderMessageEvent event = new OrderMessageEvent();
        event.setEventType(OrderEventType.RELEASE_STOCK.name());
        event.setOrderId(order.getOrderId());
        event.setUserId(order.getUserId());
        event.setOrderItems(toOrderItemDTOs(order));
        // Same key (orderId) and topic as the create event, so product-service handles it after the reservation
        outboxWriter.save(order.getOrderId(), event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findExpiredPendingOrderIds(Date createdBefore, int limit) {
        return orderRepository.findIdsByStatusCreatedBefore(OrderStatus.PENDING.name(), createdBefore,
                PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderForUser(String orderId, Long userId) {
        return orderRepository.findById(orderId)
                // 404 instead of 403 so ids of other users' orders are not revealed
                .filter(order -> Objects.equals(order.getUserId(), userId))
                .map(this::toOrderDTO)
                .orElseThrow(() -> new OrderException("Order not found", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersForUser(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserId(userId, pageable).map(this::toOrderDTO);
    }

    /** Explicit mapping: shipping info is embedded in the entity but flat in the DTO. */
    private OrderDTO toOrderDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setStatus(order.getStatus());
        dto.setUserId(order.getUserId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setVersion(order.getVersion());
        dto.setSubTotal(order.getSubTotal());
        dto.setShippingFee(order.getShippingFee());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCancelReason(order.getCancelReason());
        ShippingInfo shippingInfo = order.getShippingInfo();
        if (shippingInfo != null) {
            dto.setAddress(shippingInfo.getAddress());
            dto.setPhone(shippingInfo.getPhone());
            dto.setReceiverName(shippingInfo.getReceiverName());
            dto.setEmail(shippingInfo.getEmail());
        }
        dto.setOrderItems(order.getOrderItems().stream()
                .map(item -> modelMapper.map(item, OrderItemDTO.class))
                .collect(Collectors.toList()));
        return dto;
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
     * orderId/status/totals are ignored; prices are the server prices (already checked
     * to equal the client prices) and totals are computed by the entity.
     */
    private Order buildOrder(OrderRequestDto req, List<ValidatedItem> items, Long userId, String idempotencyKey) {
        ShippingInfo shippingInfo = new ShippingInfo(req.getAddress(), req.getPhone(), req.getReceiverName(), req.getEmail());
        Order order = Order.create(userId, idempotencyKey, shippingInfo, shippingFee, req.getPaymentMethod());
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
    private List<ValidatedItem> validateStockAndPrice(OrderRequestDto req) {
        List<OrderItemDTO> items = req.getOrderItems();
        validateItems(items);                                               // 1. input is well-formed
        Map<String, ProductResponseDto> productsById = loadProducts(items); // 2. all products exist
        checkStock(items, productsById);                                    // 3. enough stock per variant
        return checkPrices(req, productsById);                              // 4. client price must match server price
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
            // addExact: never wraps to a negative value that would pass the stock check
            int requested = requestedPerVariant.merge(variant.getId(), item.getQuantity(), Math::addExact);
            int available = Objects.requireNonNullElse(variant.getStockQuantity(), 0);
            if (available < requested) {
                LOGGER.info("Insufficient stock for variantId {}: requested {}, available {}",
                        variant.getId(), requested, available);
                throw new OrderException("Not enough stock for " + product.getName(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    /**
     * Compares the prices the customer saw with the server prices. Any difference (or a missing
     * client price) rejects the order with 409 so the customer can review the new prices;
     * an order is never charged with a client price.
     */
    private List<ValidatedItem> checkPrices(OrderRequestDto req, Map<String, ProductResponseDto> productsById) {
        List<ValidatedItem> validated = new ArrayList<>();
        Map<String, String> changedPrices = new LinkedHashMap<>();
        Set<String> changedNames = new LinkedHashSet<>();

        for (OrderItemDTO item : req.getOrderItems()) {
            ProductResponseDto product = productsById.get(item.getProductId());
            ProductVariantResponseDto variant = resolveVariant(product, item.getVariantId());
            BigDecimal serverPrice = priceOf(product, variant);
            if (item.getPrice() == null || item.getPrice().compareTo(serverPrice) != 0) {
                LOGGER.info("Price changed for variantId {}: client {}, server {}",
                        variant.getId(), item.getPrice(), serverPrice);
                changedPrices.put(item.getProductId() + ":" + item.getVariantId(), serverPrice.toPlainString());
                changedNames.add(product.getName());
            }
            validated.add(new ValidatedItem(item.getProductId(), item.getVariantId(), item.getQuantity(),
                    serverPrice, item.getColor()));
        }

        if (req.getShippingFee() != null && req.getShippingFee().compareTo(shippingFee) != 0) {
            changedPrices.put(SHIPPING_FEE_KEY, shippingFee.toPlainString());
        }

        if (!changedPrices.isEmpty()) {
            String message = changedNames.isEmpty()
                    ? "Shipping fee has changed, please review your order"
                    : "Price has changed for " + String.join(", ", changedNames) + ", please review your order";
            throw new PriceChangedException(message, changedPrices);
        }
        return validated;
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
        if (orderItems.size() > MAX_ORDER_ITEMS) {
            throw new OrderException("Order can contain at most " + MAX_ORDER_ITEMS + " items", HttpStatus.BAD_REQUEST);
        }
        for (OrderItemDTO item : orderItems) {
            if (item == null || item.getProductId() == null || item.getProductId().isBlank()) {
                throw new OrderException("Order item must have a productId", HttpStatus.BAD_REQUEST);
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderException("Item quantity must be greater than 0", HttpStatus.BAD_REQUEST);
            }
            if (item.getQuantity() > MAX_QUANTITY_PER_ITEM) {
                throw new OrderException("Item quantity must be at most " + MAX_QUANTITY_PER_ITEM, HttpStatus.BAD_REQUEST);
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
