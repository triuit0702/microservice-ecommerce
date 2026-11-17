package net.javaguides.product_service.dto.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryRequestDto {
    private String name;
    private String parentId; // null for a root category
}