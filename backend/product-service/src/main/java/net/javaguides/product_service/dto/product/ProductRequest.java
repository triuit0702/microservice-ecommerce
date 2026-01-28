package net.javaguides.product_service.dto.product;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.javaguides.product_service.dto.product_variant.ProductVariantDto;
import net.javaguides.product_service.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discount;
    private String imageUrl;
    private String imagePublicId;
    private String category;

    //private List<ProductVariantDto>  variants;
    private String variants;
}
