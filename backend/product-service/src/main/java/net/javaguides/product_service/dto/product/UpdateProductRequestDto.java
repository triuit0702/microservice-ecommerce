package net.javaguides.product_service.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.javaguides.product_service.dto.product_variant.ProductVariantDto;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProductRequestDto {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private Integer stockQuantity;
    private List<String> categoryIds;

    private int version;

    private String variants;
    private MultipartFile imageFile;
}

