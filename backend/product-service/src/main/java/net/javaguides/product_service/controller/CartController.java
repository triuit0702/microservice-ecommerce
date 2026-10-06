package net.javaguides.product_service.controller;


import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.dto.cart.AddCartRequestDto;
import net.javaguides.product_service.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/product/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Add a product to the cart.
     *
     * @param request The request containing the product details to be added to the cart.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddCartRequestDto request) {

        cartService.addToCart(request);
        return ResponseEntity.ok("Added to cart");
    }

    /**
     * Get the cart for a specific user.
     *
     * @param userId The ID of the user whose cart is to be retrieved.
     * @return A ResponseEntity containing the user's cart details.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(
                cartService.getCart(userId)
        );
    }

    /**
     * Update the quantity of a product in the cart.
     *
     * @param request The request containing the product details and the new quantity.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/update")
    public ResponseEntity<?>  updateCart(@RequestBody AddCartRequestDto request) {
        int result = cartService.updateQuantity(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Remove a product from the cart.
     *
     * @param request The request containing the product details to be removed from the cart.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/remove")
    public ResponseEntity<?> removeCartItem(@RequestBody AddCartRequestDto request) {
        cartService.removeCartItemSelected(request);
        return ResponseEntity.ok("Removed item from cart successfully");
    }

    /**
     * Delete the entire cart for a specific user.
     *
     * @param userId The ID of the user whose cart is to be deleted.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteCart(@PathVariable Long userId) {
        cartService.deleteCart(userId);
        return ResponseEntity.ok("Deleted cart successfully");
    }
}
