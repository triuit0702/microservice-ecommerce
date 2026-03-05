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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProductRequestDto {
    private String id;
    @NotBlank(message = "name is required")
    private String name;
    private String description;
    @NotBlank(message = "Image file is required")
    private String imageUrl;
    @NotNull(message = "price is required")
    private BigDecimal price;
    private Integer stockQuantity;
    @NotEmpty(message = "At least one category is required")
    private List<String> categoryIds;

    private int version;

    @Valid
    //@Valid
    @NotEmpty(message = "At least one variant is required")
    private VariantList variants;

    private MultipartFile imageFile;
}

