package net.javaguides.product_service.controller;


import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.dto.UploadResponse;
import net.javaguides.product_service.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/product/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @GetMapping
    public String hello() {
        return "Hello from Upload Service";
    }

    @PostMapping("/temp")
    public ResponseEntity<UploadResponse> uploadTemp(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        UploadResponse res = uploadService.uploadTempImage(file);
        return ResponseEntity.ok(res);
    }
}
