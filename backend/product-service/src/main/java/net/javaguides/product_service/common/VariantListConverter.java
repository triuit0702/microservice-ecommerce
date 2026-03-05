package net.javaguides.product_service.common;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javaguides.product_service.dto.product_variant.ProductVariantDto;
import net.javaguides.product_service.dto.product_variant.VariantList;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VariantListConverter implements Converter<String, VariantList> {

    private final ObjectMapper objectMapper;

    public VariantListConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public VariantList convert(String source) {
        try {
            if (source.isBlank()) {
                return new VariantList();
            }

            List<ProductVariantDto> list =
                    objectMapper.readValue(
                            source,
                            new TypeReference<List<ProductVariantDto>>() {}
                    );

            VariantList result = new VariantList();
            result.addAll(list);

            return result;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid variants JSON", e);
        }
    }
}
