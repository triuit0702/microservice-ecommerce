package net.javaguides.product_service.common;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javaguides.product_service.dto.product_variant.ProductVariantDto;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

public class VariantConverter {

    public static List<ProductVariantDto> convertStringToVariantDto(String variantStr) {
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
}
