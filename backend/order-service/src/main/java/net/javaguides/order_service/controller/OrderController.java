package net.javaguides.order_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.order_service.dto.OrderRequestDto;
import net.javaguides.order_service.dto.UserDto;
import net.javaguides.order_service.exception.OrderException;
import net.javaguides.order_service.service.AuthenticationAPIClient;
import net.javaguides.order_service.service.OrderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/order")
public class OrderController {
    private final OrderService orderService;
    private final AuthenticationAPIClient authenticationAPIClient;

    public OrderController(OrderService orderService, AuthenticationAPIClient authenticationAPIClient) {
        this.orderService = orderService;
        this.authenticationAPIClient = authenticationAPIClient;
    }



    /**
     * Endpoint to place a new order
     * @param order: Order request DTO containing order details
     * @param request: The HTTP request to extract the cookie for user authentication
     * @return ResponseEntity<ApiResponse<?>>: Response with order details or error message
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> placeOrder(@RequestBody OrderRequestDto order, HttpServletRequest request) {
        try {
            // Extract cookie from the request header
            String cookie = request.getHeader(HttpHeaders.COOKIE);

            // Send cookie in Feign request to authenticate user
            ApiResponse<UserDto> user = authenticationAPIClient.getCurrentUser(cookie).getBody();
            if (user != null && user.getData() != null) {
                // Place order with authenticated user's ID
                return new ResponseEntity<>(ApiResponse.success(orderService.placeOrder(order, user.getData().getId(), user.getData().getEmail())), HttpStatus.CREATED);
            }
            // Return if user not found
            return new ResponseEntity<>(ApiResponse.success("User not found!"), HttpStatus.NOT_FOUND);
        } catch (OrderException e) {
            // Handle custom order exceptions
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            // Handle general exceptions
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
