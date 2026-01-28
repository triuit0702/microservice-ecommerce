package net.javaguides.product_service.dto.product_variant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ProductVariantDto {


    private String sku;

    private String color;
    private String size;
    private BigDecimal price;

    private Integer stockQuantity;
    private String material;

    private Integer reorderLevel;
    public String imagePublicId;
    private String imageUrl;
}
