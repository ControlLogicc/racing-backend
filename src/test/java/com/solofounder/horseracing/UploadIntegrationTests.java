package com.solofounder.horseracing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties = "app.upload.image-dir=target/test-uploads/images")
@AutoConfigureMockMvc
class UploadIntegrationTests {

    private static final Path TEST_IMAGE_DIRECTORY =
            Path.of("target/test-uploads/images").toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void cleanupUploads() throws Exception {
        if (!Files.exists(TEST_IMAGE_DIRECTORY)) {
            return;
        }
        try (var paths = Files.walk(TEST_IMAGE_DIRECTORY)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // Test cleanup is limited to target/test-uploads.
                        }
                    });
        }
    }

    @Test
    void validImageUploadReturnsGeneratedImageUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "horse.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/uploads/images").file(file)
                        .with(user("owner@example.com").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl")
                        .value(org.hamcrest.Matchers.matchesPattern(
                                "/uploads/images/[0-9a-f-]{36}\\.png"
                        )));
    }

    @Test
    void emptyImageReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/uploads/images").file(file)
                        .with(user("jockey@example.com").roles("JOCKEY")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Image file is required"));
    }

    @Test
    void invalidImageTypeReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads/images").file(file)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only JPEG, PNG, and WEBP images are allowed"));
    }

    @Test
    void imageLargerThanFiveMegabytesReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.webp",
                "image/webp",
                new byte[5 * 1024 * 1024 + 1]
        );

        mockMvc.perform(multipart("/api/uploads/images").file(file)
                        .with(user("staff@example.com").roles("STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Image file must not exceed 5MB"));
    }

    @Test
    void uploadedImagesArePublicWithoutAuthentication() throws Exception {
        Files.createDirectories(TEST_IMAGE_DIRECTORY);
        Files.write(TEST_IMAGE_DIRECTORY.resolve("public.png"), new byte[]{1, 2, 3});

        mockMvc.perform(get("/uploads/images/public.png"))
                .andExpect(status().isOk());
    }
}
