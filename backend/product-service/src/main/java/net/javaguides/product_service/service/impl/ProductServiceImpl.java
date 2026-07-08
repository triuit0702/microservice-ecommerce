package net.javaguides.product_service.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javaguides.common_lib.dto.product.ProductDTO;
import net.javaguides.common_lib.dto.product.ProductEvent;
import net.javaguides.common_lib.dto.product.ProductMethod;
import net.javaguides.product_service.dto.*;
import net.javaguides.product_service.dto.product.*;
import net.javaguides.product_service.dto.product_variant.ProductVariantDto;
import net.javaguides.product_service.entity.Category;
import net.javaguides.product_service.entity.ProductVariant;
import net.javaguides.product_service.redis.ProductRedis;
import net.javaguides.product_service.entity.Product;
import net.javaguides.product_service.exception.ProductException;
import net.javaguides.product_service.kafka.producer.ProductProducer;
import net.javaguides.product_service.repository.ProductRepository;
import net.javaguides.product_service.repository.ProductVariantRepository;
import net.javaguides.product_service.service.CategoryService;
import net.javaguides.product_service.service.CloudinaryService;
import net.javaguides.product_service.service.ProductService;
import net.javaguides.product_service.service.ProductVariantService;
import net.javaguides.product_service.specification.ProductSpecification;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductProducer productProducer;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ModelMapper modelMapper;
    private final ProductRedis productDAO;
    private final CloudinaryService cloudinaryService;
    private final CategoryService categoryService;
    private final ProductVariantService productVariantService;


    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    public String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }
    @Override
    @Transactional
    public ProductResponseDto saveProduct(CreateProductRequestDto createProductRequestDto) {
        try {
            String publicId = System.currentTimeMillis() + "_" + createProductRequestDto.getMultipartFile().getOriginalFilename().replace(".jpg", "");
            String preUrl = "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId + "." + getFileExtension(createProductRequestDto.getMultipartFile());

            // Map DTO sang Product entity
            Product product = modelMapper.map(createProductRequestDto, Product.class);
            product.setId(UUID.randomUUID().toString());
            product.setImageUrl(preUrl);
            Product savedProduct = productRepository.save(product);

            // Lưu sản phẩm vào cache
            //productDAO.save(savedProduct);

            // Upload ảnh lên Cloudinary (nên thực hiện sau khi lưu sản phẩm thành công)
            cloudinaryService.uploadFile(createProductRequestDto.getMultipartFile(), publicId);

            return modelMapper.map(savedProduct, ProductResponseDto.class);
        } catch (Exception e) {
            throw new ProductException("Failed to create product: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }



    @Override
    public ProductResponseDto getProductById(String id) {
        LOGGER.info("Cache miss for product id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with id: " + id, HttpStatus.NOT_FOUND));


        ProductResponseDto productResponseDto = modelMapper.map(product, ProductResponseDto.class);
        List<String> categoryIds = product.getCategories().stream()
                .map(Category::getId)
                .toList();
        productResponseDto.setCategoryIds(categoryIds);
        return productResponseDto;

    }




    @Override
    public Page<ProductResponseDto> getProductList(int page, int size) {
       // Page<Product> productPage = productRepository.findAll(PageRequest.of(page, size));
        Page<Product> productPage = productRepository.findByDelFlgFalse(PageRequest.of(page, size ,
                Sort.by(Sort.Direction.DESC, "createdAt")));

//        List<ProductResponseDto> productDtos = productPage.getContent()
//                .stream()
//                .map(product -> {
//                    ProductCacheDto cachedProduct = productDAO.findByProductId(product.getId());
//
//                    if(cachedProduct == null){
//                       productDAO.save(product);
//                    }
//                    return modelMapper.map(product, ProductResponseDto.class);
//                })
//                .collect(Collectors.toList());


        List<ProductResponseDto> productDtos = productPage.getContent()
                .stream()
                .map(product -> {
                    //ProductCacheDto cachedProduct = productDAO.findByProductId(product.getId());

                  //  if(cachedProduct == null){
                        productDAO.save(product);
                   // }
                    return modelMapper.map(product, ProductResponseDto.class);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(productDtos, PageRequest.of(page, size), productPage.getTotalElements());

    }

    /**
     * Update product
     * @param id
     * @param productUpdateDto
     * @param version
     * @return
     */
    @Transactional
    @Override
    public ProductResponseDto updateProduct(String id, UpdateProductRequestDto productUpdateDto, int version) {
        // find product by id
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Not found", HttpStatus.NOT_FOUND));
        // execute update product
        Product updatedProduct = executeUpdate(productUpdateDto, product);
        return modelMapper.map(updatedProduct, ProductResponseDto.class);
    }

    /**
     * Upload main image
     * @param imageFile
     * @param product
     * @return
     * @throws IOException
     */
    private UploadResponse uploadMainImage(MultipartFile imageFile,
                                 Product product, int version) throws IOException {

        if (imageFile == null
                || imageFile.isEmpty()) {
            return null;
        }
        //int nextVersion = product.getVersion() + 1;
        String publicId =  product.getId() + "/v" + version;
        // upload main image product
        UploadResponse uploadResponse = cloudinaryService.uploadImage(imageFile, publicId);
        product.setImageUrl(uploadResponse.getUrl());
        product.setImagePublicId(uploadResponse.getPublicId());
        return uploadResponse;
    }


    /**
     * Update product
     * @param productUpdateDto
     * @param existingProduct
     * @return
     */
    public Product executeUpdate(UpdateProductRequestDto productUpdateDto , Product existingProduct)  {
        UploadResponse uploadResponse = null;
        try {
            modelMapper.typeMap(UpdateProductRequestDto.class, Product.class)
                    .addMappings(m -> m.skip(Product::setVariants));
            modelMapper.map(productUpdateDto, existingProduct);
            String oldMainImagePublicId = existingProduct.getImagePublicId();
            // upload main image product
            uploadResponse = uploadMainImage(productUpdateDto.getImageFile(), existingProduct, existingProduct.getVersion() + 1);

            // attach categories
            attachCategories(existingProduct, productUpdateDto.getCategoryIds());
            // save product
            existingProduct = productRepository.save(existingProduct);

            // delete old variants
            productVariantService.deleteAllByProductId(existingProduct.getId());

            List<ProductVariantDto> variants = productUpdateDto.getVariants();
            if (CollectionUtils.isEmpty(variants)) {
                return existingProduct;
            }
            createVariants(variants, existingProduct);
            // all success → delete main image product old
            deleteImageAfterCommit(oldMainImagePublicId);
        } catch (Exception  e) {
            log.error("Update product failed, productId={}", existingProduct.getId(), e);
            rollbackImage(uploadResponse);

            throw new ProductException(
                    "Error processing product update",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
        return existingProduct;
    }

    /**
     * Đăng ký xóa ảnh sau khi transaction commit thành công để tránh trường hợp xóa ảnh nhưng transaction rollback do lỗi DB
     * @param oldPublicId
     */
    private void deleteImageAfterCommit(String oldPublicId) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            cloudinaryService.deleteImage(oldPublicId);
                        } catch (Exception e) {
                            log.error("Delete old image failed: {}", oldPublicId, e);
                        }
                    }
                }
        );
    }

    private List<ProductVariantDto> convertStringToVariantDto(String variantStr) {
        List<ProductVariantDto> variants = new ArrayList<>();

        if (StringUtils.isNotBlank(variantStr)) {
            try {
                variants = new ObjectMapper().readValue(
                        variantStr,
                        new TypeReference<List<ProductVariantDto>>() {}
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return variants;
    }

    @Override
    public void deleteProduct(String id) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id, HttpStatus.NOT_FOUND));

        existingProduct.setDelFlg(true);
        productRepository.save(existingProduct);
    }


    @Override
    public List<ProductResponseDto> getProductsByIds(Set<String> productIds) {
        return productRepository.findAllByIdIn(productIds)
                .stream()
                .map(product -> modelMapper.map(product, ProductResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponseDto> searchProducts(String name, String categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(null);

        if (name != null && !name.isEmpty()) {
            spec = spec.and(ProductSpecification.hasName(name));
        }

        if (categoryId != null && !categoryId.isEmpty()) {
            spec = spec.and(ProductSpecification.inCategory(categoryId));
        }

        if (minPrice != null && maxPrice != null) {
            spec = spec.and(ProductSpecification.hasPriceBetween(minPrice, maxPrice));
        }

        Page<Product> products = productRepository.findAll(spec, pageable);

        // Sử dụng map() của Page để chuyển đổi từng Product thành ProductResponseDto
        return products.map(product -> modelMapper.map(product, ProductResponseDto.class));
    }



    // Private Helper Methods
    private Product mapToEntity(ProductDTO productDTO) {
        return modelMapper.map(productDTO, Product.class);
    }

    private ProductEvent createProductEvent(Product product, int stockQuantity, ProductMethod method) {
        ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
        productDTO.setStockQuantity(stockQuantity);

        ProductEvent productEvent = new ProductEvent();
        productEvent.setProductDTO(productDTO);
        productEvent.setMethod(method);
        return productEvent;
    }

    private ProductEvent createProductEvent(ProductDTO productDTO, ProductMethod method) {
        ProductEvent productEvent = new ProductEvent();
        productEvent.setProductDTO(productDTO);
        productEvent.setMethod(method);
        return productEvent;
    }






    /**
     * Create product
     * @param req
     * @param image
     * @throws IOException
     */
    @Transactional
    public void createProduct(ProductRequest req,  MultipartFile image) throws IOException {
        List<ProductVariantDto> variants = req.getVariants();
        UploadResponse uploadResponse = null;
        try {
            // 1. save product
            Product product = saveProduct(req);

            // upload main image product
            uploadResponse = uploadMainImage(image, product, 0);

            // attach categories
            attachCategories(product, req.getCategoryIds());

            if (!CollectionUtils.isEmpty(variants)) {
                // create product variants
                createVariants(variants, product);
            }
        } catch (Exception e) {
            // 3. Rollback ảnh nếu DB fail
           rollbackImage(uploadResponse);
            log.error("Create product failed", e);
            throw new ProductException(
                    "Error processing create product",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            ); // cho transaction rollback DB
        }

    }

    /**
     * save product to DB
     * @param req
     * @return
     */
    private Product saveProduct(ProductRequest req) {
        Product product = buildProduct(req);
        return productRepository.save(product);
    }

    /**
     * attach categories to product
     * @param product
     * @param categoryIds
     */
    private void attachCategories(Product product, List<String> categoryIds) {
        // get category
        Set<Category> categories = categoryService.getByCategoryIdList(categoryIds);
        if (!CollectionUtils.isEmpty(categories)) {
            product.setCategories(categories);
        }
    }

    /**
     * Rollback image if  DB fail
     * @param uploadResponse
     */
    private void rollbackImage(UploadResponse uploadResponse) {
        if (uploadResponse != null && uploadResponse.getPublicId() != null) {
            try {
                cloudinaryService.deleteImage(uploadResponse.getPublicId());
            } catch (Exception ex) {
                log.error("Rollback image failed, publicId={}", uploadResponse.getPublicId(), ex);
            }
        }
    }

    /**
     * Build Product entity từ ProductRequest DTO
     * @param req
     * @return
     */
    private Product buildProduct(ProductRequest req) {
        Product product = new Product();
        product.setId(UUID.randomUUID().toString());
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setDiscount(req.getDiscount());
        return product;
    }

    /**
     * Create variants cho product
     * @param variants
     * @param product
     */
    private void createVariants(List<ProductVariantDto> variants, Product product) {

        List<ProductVariant> productVariantList = new ArrayList<>();
        List<String> oldPublicIds = new ArrayList<>();
        // 2. Loop variants
        for (ProductVariantDto vReq : variants) {

            ProductVariant variant = buildProductVariant(vReq, product);
            productVariantList.add(variant);
            oldPublicIds.add(vReq.getImagePublicId());
        }

        // save list variant to DB
        List<ProductVariant> savedVariants = productVariantRepository.saveAllAndFlush(productVariantList);
        List<Long> ids = savedVariants.stream().map(ProductVariant::getId).toList();
        for (int i=0; i < savedVariants.size(); i ++) {
            ProductVariant variant = savedVariants.get(i);
            String oldPublicId = oldPublicIds.get(i);

            // 3. Rename ảnh temp → permanent
            cloudinaryService.updateImageTags(
                    oldPublicId
            );

            variant.setImageUrl(cloudinaryService.getUrlByPublicId(oldPublicId));
        }

        // update db
        productVariantRepository.saveAll(savedVariants);
    }

    /**
     * Build ProductVariant entity from ProductVariantDto
     * @param vReq
     * @param product
     * @return
     */
    private ProductVariant buildProductVariant(ProductVariantDto vReq, Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setSku(vReq.getSku());
        variant.setPrice(vReq.getPrice());
        variant.setColor(vReq.getColor());
        variant.setSize(vReq.getSize());
        variant.setMaterial(vReq.getMaterial());
        variant.setStockQuantity(vReq.getStockQuantity());
        variant.setProduct(product);
        variant.setImagePublicId(vReq.getImagePublicId());
        return variant;
    }

    public List<Product> findAllByListProductId(List<String> ids) {
        return productRepository.findByIdIn(ids);
    }
}
