package net.javaguides.product_service.service;



import net.javaguides.product_service.dto.category.CategoryResponseDto;
import net.javaguides.product_service.dto.category.CreateCategoryRequestDto;
import net.javaguides.product_service.entity.Category;
import net.javaguides.product_service.entity.Product;

import java.util.List;
import java.util.Set;

public interface CategoryService {
    CategoryResponseDto createCategory(CreateCategoryRequestDto requestDto);
    CategoryResponseDto updateCategory(String id, CreateCategoryRequestDto requestDto);
    void deleteCategory(String id);
    CategoryResponseDto getCategoryById(String id);
    List<CategoryResponseDto> getAllCategories();
    List<CategoryResponseDto> getRootCategories(); // Danh mục gốc cho mega menu
    Category getById(String id);
    Set<Category> getByCategoryIdList(List<String> idList);
    void attachCategories(Product product, List<String> categoryIds);
}
