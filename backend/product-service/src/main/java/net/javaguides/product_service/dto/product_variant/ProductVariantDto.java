package net.javaguides.product_service.dto.product_variant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ProductVariantDto {

    @NotBlank (message = "sku is required")
    private String sku;

    @NotBlank(message = "color is required")
    private String color;
    @NotBlank(message = "size is required")
    private String size;
    @NotNull(message = "price is required")
    private BigDecimal price;

    @NotNull(message = "stock quantity is required")
    private Integer stockQuantity;
    private String material;

    public String imagePublicId;
    private Integer reorderLevel;
    @NotBlank(message = "image is required")
    private String imageUrl;
}
