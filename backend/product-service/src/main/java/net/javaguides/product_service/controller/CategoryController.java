package net.javaguides.product_service.controller;

import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.product_service.dto.category.CategoryResponseDto;
import net.javaguides.product_service.dto.category.CreateCategoryRequestDto;
import net.javaguides.product_service.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product/categories")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * Create a new category
     *
     * @param requestDto the request DTO containing category details
     * @return the created category response DTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(@RequestBody CreateCategoryRequestDto requestDto) {
        CategoryResponseDto category = categoryService.createCategory(requestDto);
        return new ResponseEntity<>(ApiResponse.success(category), HttpStatus.CREATED);
    }

    /**
     * Update an existing category
     *
     * @param id         the ID of the category to update
     * @param requestDto the request DTO containing updated category details
     * @return the updated category response DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(@PathVariable String id, @RequestBody CreateCategoryRequestDto requestDto) {
        CategoryResponseDto category = categoryService.updateCategory(id, requestDto);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * Delete a category by ID
     *
     * @param id the ID of the category to delete
     * @return a response entity with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Get a category by ID
     *
     * @param id the ID of the category to retrieve
     * @return the category response DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategoryById(@PathVariable String id) {
        CategoryResponseDto category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * Get all categories
     *
     * @return a list of category response DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllCategories() {
        List<CategoryResponseDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
