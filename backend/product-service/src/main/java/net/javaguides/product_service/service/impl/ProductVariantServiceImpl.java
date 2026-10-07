package net.javaguides.product_service.service.impl;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.exception.ResourceNotFoundException;
import net.javaguides.product_service.dto.product_variant.ProductVariantResponseDto;
import net.javaguides.product_service.dto.product_variant.UpdateProductVariantRequestDto;
import net.javaguides.product_service.entity.Product;
import net.javaguides.product_service.entity.ProductVariant;
import net.javaguides.product_service.redis.ProductRedis;
import net.javaguides.product_service.repository.ProductRepository;
import net.javaguides.product_service.repository.ProductVariantRepository;
import net.javaguides.product_service.service.ProductVariantService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository productVariantRepository;


    private final ProductRepository productRepository;

    private final ModelMapper modelMapper;

    private final ProductRedis productRedis;

    @Transactional
    @Override
    public ProductVariantResponseDto createProductVariant(String productId, BigDecimal price, String sku, Integer initialStock, Integer reorderLevel) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setPrice(price);
        variant.setSku(sku);
        variant.setStockQuantity(initialStock);
        variant.setReorderLevel(reorderLevel);

        ProductVariant savedVariant = productVariantRepository.save(variant);

        product.getVariants().add(savedVariant);
        //productRedis.save(product);

        return modelMapper.map(savedVariant, ProductVariantResponseDto.class);
    }

    @Override
    public List<ProductVariantResponseDto> getVariantsByProductId(String productId) {
        return productVariantRepository.findByProductId(productId)
                .stream()
                .map(productVariant -> modelMapper.map(productVariant, ProductVariantResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantResponseDto updateProductVariant(Long variantId, UpdateProductVariantRequestDto updateDTO) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant not found"));

        // Cập nhật các thuộc tính cơ bản
        if (updateDTO.getPrice() != null) {
            variant.setPrice(updateDTO.getPrice());
        }
        if (updateDTO.getSku() != null) {
            variant.setSku(updateDTO.getSku());
        }
        if (updateDTO.getStockQuantity() != null) {
            variant.setStockQuantity(updateDTO.getStockQuantity());
        }
        if (updateDTO.getReorderLevel() != null) {
            variant.setReorderLevel(updateDTO.getReorderLevel());
        }

        ProductVariant updatedVariant = productVariantRepository.save(variant);
        return modelMapper.map(updatedVariant, ProductVariantResponseDto.class);
    }

    /**
     * Xóa một ProductVariant
     */
    @Transactional
    public void deleteProductVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant not found"));

        Product product = variant.getProduct();

        // Xóa ProductVariant
        productVariantRepository.delete(variant);

        // Gọi Stock Service để xóa tồn kho
//        stockClient.deleteStock(variantId);

        // Cập nhật cache cho sản phẩm
        productRedis.save(product);
    }

    @Override
    public void saveProductVariant(ProductVariant productVariant) {
        productVariantRepository.save(productVariant);
    }

    @Override
    public int deleteByProductId(String productId) {
        return productVariantRepository.deleteByProductId(productId);
    }

    @Override
    public int deleteAllByProductId(String productId) {
        int total =  productVariantRepository.deleteAllByProductId(productId);
        productVariantRepository.flush();
        return total;
    }

    /**
     * Lưu danh sách ProductVariant vào cơ sở dữ liệu
     * @param productVariants: Danh sách các ProductVariant cần lưu
     */
    @Override
    public void saveAllListVariant(List<ProductVariant> productVariants) {
        productVariantRepository.saveAll(productVariants);
    }


    @Override
    public List<ProductVariant> getProductVariantByIds(Set<Long> variantIds) {
        return productVariantRepository.findAllByIdIn(variantIds);
    }

    @Override
    public ProductVariant getVariantById(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId).orElseThrow(()
                -> new ResourceNotFoundException("Product variant not found with ID: " + variantId));
        return variant;
    }

    // Các phương thức CRUD khác như update, delete nếu cần
}
