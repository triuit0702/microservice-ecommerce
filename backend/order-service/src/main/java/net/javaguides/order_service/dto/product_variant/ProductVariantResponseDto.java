package net.javaguides.order_service.dto.product_variant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantResponseDto {
    private Long id;
    private BigDecimal price;
    private String sku;
    private Integer stockQuantity;
    private Integer reorderLevel;
}
