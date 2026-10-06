package net.javaguides.product_service.controller;


import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.common_lib.dto.product.ProductDTO;
import net.javaguides.product_service.dto.product.CreateProductRequestDto;
import net.javaguides.product_service.dto.ProductStockResponse;
import net.javaguides.product_service.dto.product.ProductRequest;
import net.javaguides.product_service.dto.product.ProductResponseDto;
import net.javaguides.product_service.dto.product.UpdateProductRequestDto;
import net.javaguides.product_service.dto.product_variant.ProductVariantDto;
import net.javaguides.product_service.exception.ProductException;
import net.javaguides.product_service.service.ProductService;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api/v1/product/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    /**
     * Get a paginated list of products.
     *
     * @param page the page number (default is 0)
     * @param size the page size (default is 10)
     * @return a ResponseEntity containing the ApiResponse with the product list
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getProductList(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ProductResponseDto> productList = productService.getProductList(page, size);
        return new ResponseEntity<>(ApiResponse.success(productList), HttpStatus.OK);

    }

    /**
     * Get a product by  ID.
     *
     * @param id the ID of the product
     * @return a ResponseEntity containing the ApiResponse with the product details
     */
    @GetMapping("{id}")
    public ResponseEntity<ApiResponse<?>> getProductById(@PathVariable("id") String id) {
            ProductResponseDto productStockResponse = productService.getProductById(id);
            return new ResponseEntity<>(ApiResponse.success(productStockResponse), HttpStatus.OK);

    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateProduct(@PathVariable("id") String id,
                                                        @Valid @ModelAttribute UpdateProductRequestDto productDTO) throws Exception {
        ProductResponseDto productStockResponse = productService.updateProduct(
                id,
                productDTO,
                productDTO.getVersion());
        return new ResponseEntity<>(ApiResponse.success(productStockResponse), HttpStatus.OK);

    }

    @DeleteMapping("{id}")
    public ResponseEntity<ApiResponse<?>> deleteProduct(@PathVariable("id") String id) {
        productService.deleteProduct(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

//    @GetMapping("/search")
//    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) String categoryId,
//            @RequestParam(required = false) BigDecimal minPrice,
//            @RequestParam(required = false) BigDecimal maxPrice,
//            Pageable pageable) {
//
//        Page<ProductResponseDto> products = productService.searchProducts(name, categoryId, minPrice, maxPrice, pageable);
//        return ResponseEntity.ok(ApiResponse.success(products));
//    }



    @GetMapping("/by-ids")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsByIds(@RequestParam("ids") Set<String> productIds) {
        List<ProductResponseDto> productDTOs = productService.getProductsByIds(productIds);
        return new ResponseEntity<>(ApiResponse.success(productDTOs), HttpStatus.OK);

    }



    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> create(@Valid @ModelAttribute ProductRequest request,
                                       @RequestParam(required = false) MultipartFile imageFile)
            throws Exception {

       productService.createProduct(request, imageFile);
        return ResponseEntity.ok().build();
    }



}
