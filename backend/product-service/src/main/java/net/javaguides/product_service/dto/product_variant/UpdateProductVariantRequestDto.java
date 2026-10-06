package net.javaguides.product_service.dto.product_variant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProductVariantRequestDto {
    private BigDecimal price;
    private String sku;
    private Integer stockQuantity;
    private Integer reorderLevel;
}
