package net.javaguides.product_service.dto.cart;


import lombok.Data;

@Data
public class AddCartRequestDto {
    private String productId;
    private Integer quantity;
    private Long userId;
}
