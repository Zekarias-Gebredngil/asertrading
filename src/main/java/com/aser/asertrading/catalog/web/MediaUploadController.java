package com.aser.asertrading.catalog.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.storage.S3StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media & Assets", description = "Endpoints for persistent media upload and cloud storage")
public class MediaUploadController {

    private final S3StorageService storageService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image and return persistent S3 or cloud URL")
    public ApiResponse<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "images") String folder) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "image.png"));
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file format. Allowed types: jpg, jpeg, png, webp, gif, svg");
        }

        String safeFolderName = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        if (safeFolderName.isBlank()) {
            safeFolderName = "images";
        }

        try {
            String uniqueName = UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis() + "." + extension;
            byte[] fileBytes = file.getBytes();

            S3StorageService.StorageResult result = storageService.store(
                    safeFolderName,
                    uniqueName,
                    file.getInputStream(),
                    fileBytes,
                    file.getContentType()
            );

            Map<String, String> response = new HashMap<>();
            response.put("url", result.url());
            response.put("relativeUrl", result.relativeUrl());
            response.put("fileName", uniqueName);
            response.put("originalName", originalFilename);
            response.put("storageType", result.storageType());

            log.info("Media stored via {} at: {}", result.storageType(), result.url());
            return ApiResponse.success("Image uploaded successfully via " + result.storageType(), response);

        } catch (IOException ex) {
            log.error("Failed to store uploaded media file", ex);
            throw new RuntimeException("Failed to persist uploaded image: " + ex.getMessage());
        }
    }
}
