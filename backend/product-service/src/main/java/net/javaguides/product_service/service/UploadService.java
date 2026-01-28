package net.javaguides.product_service.service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.dto.UploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final Cloudinary cloudinary;

    public UploadResponse uploadTempImage(MultipartFile file) throws Exception {

        // Validate
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String uuid = UUID.randomUUID().toString();

        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "products/temp",
                        "public_id", uuid,
                        "tags", "temp",
                        "resource_type", "image"
                )
        );

        return UploadResponse.builder()
                .url((String) result.get("secure_url"))
                .publicId((String) result.get("public_id"))
                .build();
    }
}
