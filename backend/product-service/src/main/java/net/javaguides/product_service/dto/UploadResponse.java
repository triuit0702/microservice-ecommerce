package net.javaguides.product_service.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {
    private String url;
    private String publicId;
}
