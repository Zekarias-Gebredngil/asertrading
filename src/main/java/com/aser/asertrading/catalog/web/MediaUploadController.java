package com.aser.asertrading.catalog.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media & Assets", description = "Endpoints for persistent media upload and storage")
public class MediaUploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.media.base-url:http://localhost:8085}")
    private String mediaBaseUrl;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image and return persistent URL")
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
            Path targetDir = Paths.get(uploadDir, safeFolderName).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String uniqueName = UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis() + "." + extension;
            Path destination = targetDir.resolve(uniqueName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativeUrl = "/uploads/" + safeFolderName + "/" + uniqueName;
            String absoluteUrl = (mediaBaseUrl.endsWith("/") ? mediaBaseUrl.substring(0, mediaBaseUrl.length() - 1) : mediaBaseUrl) + relativeUrl;
            log.info("Media stored permanently at: {}", destination);

            Map<String, String> response = new HashMap<>();
            response.put("url", absoluteUrl);
            response.put("relativeUrl", relativeUrl);
            response.put("fileName", uniqueName);
            response.put("originalName", originalFilename);

            return ApiResponse.success("Image uploaded successfully", response);

        } catch (IOException ex) {
            log.error("Failed to store uploaded media file", ex);
            throw new RuntimeException("Failed to persist uploaded image: " + ex.getMessage());
        }
    }
}
