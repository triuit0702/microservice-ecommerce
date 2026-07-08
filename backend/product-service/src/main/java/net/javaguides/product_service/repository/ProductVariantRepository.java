package net.javaguides.product_service.repository;

import net.javaguides.product_service.entity.Product;
import net.javaguides.product_service.entity.ProductVariant;
import net.javaguides.product_service.projection.CartItemView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findAllByIdIn(Set<Long> variantIds);
    List<ProductVariant> findByProductId(String productId);

    @Modifying
    @Transactional
    int deleteByProductId(String productId);

    int deleteAllByProductId(String productId);

    List<ProductVariant> findByIdIn(List<Long> ids);

    // get cart item
    @Query("""
    SELECT
        p.name as productName,
        v.id as variantId,
        v.color as color,
        v.price as price,
        v.imageUrl as imageUrl
    FROM ProductVariant v
    JOIN v.product p
    WHERE v.id IN :variantIds
    """)
    List<CartItemView> getCartItems(List<Long> variantIds);
}