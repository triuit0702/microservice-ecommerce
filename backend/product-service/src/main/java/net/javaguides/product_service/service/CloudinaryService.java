package net.javaguides.product_service.service;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javaguides.product_service.dto.UploadResponse;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private static final Logger jobLog =
            LoggerFactory.getLogger("JOB");

    private final Cloudinary cloudinary;

    @Async
    public void uploadFile(MultipartFile file, String publicId) {
        try {
            // Đọc ảnh từ MultipartFile thành BufferedImage
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // Resize ảnh, ví dụ resize width về 800px (giữ nguyên tỷ lệ)
            BufferedImage resizedImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, 800);

            // Chuyển ảnh resized thành byte[]
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", outputStream);
            byte[] resizedBytes = outputStream.toByteArray();

            // Upload ảnh đã resize lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(resizedBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "quality", "auto:good" // Sử dụng nén tự động với chất lượng tốt
            ));

            // Tạo URL mà không chứa version để tiện lợi
            String url = cloudinary.url().generate(uploadResult.get("public_id").toString());

            log.info("Uploaded file URL: {}", url);

        } catch (IOException e) {
            log.error("IO Exception during file upload", e);
        } catch (Exception e) {
            log.error("Unexpected exception during file upload", e);
        }
    }

    // rename image: change folder image
    public String renameImage(String publicId) {
        try {
//            Map result = cloudinary.uploader().rename(oldPublicId, newPublicId,  ObjectUtils.asMap(
//                    "overwrite", true,
//                    "asset_folder", "products/variants"
//
//            ));

            String[] tags = {"temp", "unsaved"};
            String[] publicIdArr = {publicId};

            cloudinary.uploader().removeTag(tags,
                    publicIdArr,
                    ObjectUtils.emptyMap());


            String[] newTags = {"product", "saved"};
            cloudinary.uploader().addTag(
                    newTags,
                    publicIdArr,
                    ObjectUtils.emptyMap());


//            cloudinary.uploader().explicit(
//                    publicId,
//                    ObjectUtils.asMap(
//                            "type", "upload",
//                            "asset_folder", "products/variants"
//                    )
//            );


            return publicId;
        } catch (IOException e) {
            throw new RuntimeException("Rename image failed", e);
        }
    }

    // get url by public id
    public String getUrlByPublicId(String publicId) {
        return cloudinary.url().secure(true).generate(publicId);
    }

    public UploadResponse uploadImageToFolder(MultipartFile file, String folder, String publicId) throws IOException {

        // Validate
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }


        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "public_id", publicId,
                        "tags", "product",
                        "resource_type", "image"
                )
        );

        return UploadResponse.builder()
                .url((String) result.get("secure_url"))
                .publicId((String) result.get("public_id"))
                .build();
    }

    public UploadResponse uploadImageExisting(MultipartFile file, String publicId) throws IOException {

        // Validate
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Map<String, Object> result = Map.of();

        try {
            result = cloudinary.uploader().upload(file.getBytes(),  Map.of(
                    "public_id", publicId,
                    "overwrite", true,
                    "invalidate", true // cực kỳ quan trọng
            ));
        } catch (Exception e) {
            log.error("Failed update  image: {}", publicId, e);
        }

        return UploadResponse.builder()
                .url((String) result.get("secure_url"))
                .publicId((String) result.get("public_id"))
                .build();
    }

    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image")
            );
        } catch (Exception e) {
            log.error("Failed to delete image: {}", publicId, e);
        }
    }

    // remove temp image
    public void deleteTempImage(String trigger) {
        String jobId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();


        MDC.put("jobId", jobId);
        MDC.put("jobName", "remove-data");
        MDC.put("trigger", trigger);




        try {

            log.info("JOB_START");

            // mốc thời gian: ảnh cũ hơn 24h sẽ bị xoá
            Instant cutoffTime = Instant.now().minus(24, ChronoUnit.HOURS);
            Map result = cloudinary.search().expression("folder:products/temp AND tags:unsaved")
                            .sortBy("created_at", "asc")
                                    .maxResults(100)
                                            .execute();

            List<Map> resources = (List<Map>) result.get("resources");

            for (Map image: resources) {
                String publicId = (String) image.get("public_id");
                String createdAt = (String) image.get("created_at");

                Instant createdTime = Instant.parse(createdAt);
                if (createdTime.isBefore(cutoffTime)) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            }

            log.info("JOB_SUCCESS removedCount={}", resources.size());
        } catch (Exception e) {
            log.error("JOB_FAILED", e);
        } finally {
            log.info("JOB_END durationMs={}", System.currentTimeMillis() - start);
            MDC.clear();
        }
    }
}
