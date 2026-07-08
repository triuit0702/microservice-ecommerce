package net.javaguides.product_service.dto.cart;


import lombok.Data;

@Data
public class AddCartRequestDto {
    private String productId;
    private String variantId;
    private Integer quantity;
    private String color;
    private String imageUrl;
    private Long userId;
}
