package net.javaguides.product_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;



    private BigDecimal discount;
// SKU là mã định danh DUY NHẤT cho từng sản phẩm / từng variant trong kho
    @Column(nullable = false, unique = true)
    private String sku;

    private String color;
    private String size;
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;
    // image url ???

    private String material;

    //@Column(nullable = false)
    private Integer reorderLevel;

    private String imagePublicId;
    private String imageUrl;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttributeValue> attributeValues = new HashSet<>();
}
