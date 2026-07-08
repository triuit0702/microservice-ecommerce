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

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddCartRequestDto request) {

        cartService.addToCart(request);
        return ResponseEntity.ok("Added to cart");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(
                cartService.getCart(userId)
        );
    }

    @PostMapping("/update")
    public ResponseEntity<?>  updateCart(@RequestBody AddCartRequestDto request) {
        int result = cartService.updateQuantity(request);
        return ResponseEntity.ok(result);
    }
}
