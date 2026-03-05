package net.javaguides.product_service.config;


import net.javaguides.product_service.common.VariantListConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VariantListConverter converter;

    public WebConfig(VariantListConverter converter) {
        this.converter = converter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(converter);
    }
}
