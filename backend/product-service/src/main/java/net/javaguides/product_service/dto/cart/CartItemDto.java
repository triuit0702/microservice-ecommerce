package net.javaguides.product_service.dto.cart;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {

    private String id;
    private Long variantId;
    private String color;

    private String name;

    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
}
