package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.upload.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, Set<String>> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/jpg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp")
    );

    private final Path imageDirectory;

    public UploadService(@Value("${app.upload.image-dir:uploads/images}") String imageDirectory) {
        this.imageDirectory = Paths.get(imageDirectory).toAbsolutePath().normalize();
    }

    public UploadResponse uploadImage(MultipartFile file) {
        validateImage(file);

        String extension = resolveExtension(file);
        String fileName = UUID.randomUUID() + "." + extension;
        Path destination = imageDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(imageDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file name");
        }

        try {
            Files.createDirectories(imageDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store image", ex);
        }

        return UploadResponse.builder()
                .imageUrl("/uploads/images/" + fileName)
                .build();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file must not exceed 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPEG, PNG, and WEBP images are allowed"
            );
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        int dotIndex = originalName == null ? -1 : originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file extension is required");
        }

        String extension = originalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.get(contentType).contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image extension does not match content type");
        }
        return extension;
    }
}
