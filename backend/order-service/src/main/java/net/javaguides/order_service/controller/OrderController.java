package net.javaguides.order_service.controller;

import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.order_service.dto.OrderRequestDto;
import net.javaguides.order_service.exception.OrderException;
import net.javaguides.order_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/order")
public class OrderController {
    // Set by api-gateway (AuthenticationFilter) from the JWT; client-supplied values are stripped there.
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Endpoint to place a new order
     * @param order: Order request DTO containing order details
     * @param userId: authenticated user's ID, set by api-gateway
     * @param email: authenticated user's email, set by api-gateway
     * @return ResponseEntity<ApiResponse<?>>: Response with order details or error message
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> placeOrder(@RequestBody OrderRequestDto order,
                                                     @RequestHeader(value = USER_ID_HEADER, required = false) Long userId,
                                                     @RequestHeader(value = USER_EMAIL_HEADER, required = false) String email) {
        if (userId == null) {
            return new ResponseEntity<>(ApiResponse.error("Unauthorized"), HttpStatus.UNAUTHORIZED);
        }
        try {
            // Place order with authenticated user's ID
            return new ResponseEntity<>(ApiResponse.success(orderService.placeOrder(order, userId, email)), HttpStatus.CREATED);
        } catch (OrderException e) {
            // Handle custom order exceptions
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            // Handle general exceptions
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
