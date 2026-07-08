package net.javaguides.product_service.dto.cart;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CartResponseDto {
    private String userId;
    private List<CartItemDto> items;
}
