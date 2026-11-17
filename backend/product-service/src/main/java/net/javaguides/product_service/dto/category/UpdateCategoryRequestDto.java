package net.javaguides.product_service.dto.category;

import lombok.Data;

@Data
public class UpdateCategoryRequestDto {
    private String id;
    private String name;
    private String parentId; // null for a root category
}