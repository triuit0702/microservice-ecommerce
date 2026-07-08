package net.javaguides.product_service.projection;

import java.math.BigDecimal;

public interface CartItemView {
    //String getProductId();
    Long getVariantId();
    String getColor();
    String getProductName();
    String getImageUrl();
    BigDecimal getPrice();
   // Integer getQuantity();
}
