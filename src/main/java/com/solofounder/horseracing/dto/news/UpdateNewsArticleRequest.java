package com.solofounder.horseracing.dto.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNewsArticleRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Size(max = 1000, message = "Summary must be at most 1000 characters")
    private String summary;

    private String content;

    @Size(max = 2048, message = "Thumbnail URL must be at most 2048 characters")
    @Pattern(regexp = "^(https?://\\S+|/uploads/images/[^\\s]+)?$",
            message = "Thumbnail URL must be an HTTP URL or uploaded image path")
    private String thumbnailUrl;

    @Size(max = 2048, message = "External link must be at most 2048 characters")
    @Pattern(regexp = "^(https?://\\S+)?$", message = "External link must be a valid HTTP URL")
    private String externalLink;

    private LocalDateTime publishDate;

    @Pattern(regexp = "^(?i:draft|published|hidden|deleted)$",
            message = "Status must be draft, published, hidden, or deleted")
    private String status;
}
