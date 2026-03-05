package net.javaguides.product_service.controller;


import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.dto.cart.AddCartRequestDto;
import net.javaguides.product_service.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddCartRequestDto request) {

        cartService.addToCart(
                request.getUserId(),
                request.getProductId(),
                request.getQuantity()
        );

        return ResponseEntity.ok("Added to cart");
    }

    @GetMapping
    public ResponseEntity<?> getCart(Long userId) {
        return ResponseEntity.ok(
                cartService.getCart(userId)
        );
    }
}
