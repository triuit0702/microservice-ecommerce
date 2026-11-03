package net.javaguides.product_service.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.dto.cart.AddCartRequestDto;
import net.javaguides.product_service.exception.CartAccessException;
import net.javaguides.product_service.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("api/v1/product/cart")
@RequiredArgsConstructor
public class CartController {

    // Set by api-gateway (AuthenticationFilter) from the JWT; client-supplied values are stripped there.
    private static final String USER_ID_HEADER = "X-User-Id";

    private final CartService cartService;

    /**
     * Add a product to the cart.
     * The userId in the body is ignored and replaced by the authenticated user's ID.
     *
     * @param request The request containing the product details to be added to the cart.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddCartRequestDto request, HttpServletRequest httpRequest) {
        request.setUserId(resolveCurrentUserId(httpRequest));
        cartService.addToCart(request);
        return ResponseEntity.ok("Added to cart");
    }

    /**
     * Get the cart of the authenticated user.
     *
     * @param userId The ID of the user whose cart is to be retrieved; must match the authenticated user.
     * @return A ResponseEntity containing the user's cart details.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId, HttpServletRequest httpRequest) {
        ensureOwnCart(userId, httpRequest);
        return ResponseEntity.ok(
                cartService.getCart(userId)
        );
    }

    /**
     * Update the quantity of a product in the cart.
     * The userId in the body is ignored and replaced by the authenticated user's ID.
     *
     * @param request The request containing the product details and the new quantity.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/update")
    public ResponseEntity<?> updateCart(@RequestBody AddCartRequestDto request, HttpServletRequest httpRequest) {
        request.setUserId(resolveCurrentUserId(httpRequest));
        int result = cartService.updateQuantity(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Remove a product from the cart.
     * The userId in the body is ignored and replaced by the authenticated user's ID.
     *
     * @param request The request containing the product details to be removed from the cart.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/remove")
    public ResponseEntity<?> removeCartItem(@RequestBody AddCartRequestDto request, HttpServletRequest httpRequest) {
        request.setUserId(resolveCurrentUserId(httpRequest));
        cartService.removeCartItemSelected(request);
        return ResponseEntity.ok("Removed item from cart successfully");
    }

    /**
     * Delete the entire cart of the authenticated user.
     *
     * @param userId The ID of the user whose cart is to be deleted; must match the authenticated user.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteCart(@PathVariable Long userId, HttpServletRequest httpRequest) {
        ensureOwnCart(userId, httpRequest);
        cartService.deleteCart(userId);
        return ResponseEntity.ok("Deleted cart successfully");
    }

    /**
     * Get the authenticated user's ID from the X-User-Id header set by api-gateway after JWT validation.
     */
    private Long resolveCurrentUserId(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new CartAccessException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new CartAccessException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Reject access to a cart that does not belong to the authenticated user.
     */
    private void ensureOwnCart(Long userId, HttpServletRequest httpRequest) {
        Long currentUserId = resolveCurrentUserId(httpRequest);
        if (!Objects.equals(currentUserId, userId)) {
            throw new CartAccessException("You cannot access another user's cart", HttpStatus.FORBIDDEN);
        }
    }
}
