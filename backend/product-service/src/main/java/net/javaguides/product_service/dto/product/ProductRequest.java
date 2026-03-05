package net.javaguides.product_service.dto.product;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.javaguides.product_service.dto.product_variant.ProductVariantDto;
import net.javaguides.product_service.dto.product_variant.VariantList;
import net.javaguides.product_service.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {
    @NotBlank(message = "name is required")
    private String name;
    private String description;
    @NotNull(message = "price is required")
    private BigDecimal price;
    private BigDecimal discount;
    //@NotBlank(message = "image product is required")
    private String imageUrl;
    private String imagePublicId;
    private String category;
    @NotEmpty(message = "At least one category is required")
    private List<String> categoryIds;

    //private List<ProductVariantDto>  variants;
    @Valid
    //@Valid
    @NotEmpty(message = "At least one variant is required")
    private VariantList variants;
}
