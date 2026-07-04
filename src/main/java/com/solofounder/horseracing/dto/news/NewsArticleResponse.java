package com.solofounder.horseracing.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleResponse {
    private Long newsId;
    private String title;
    private String summary;
    private String content;
    private String thumbnailUrl;
    private String externalLink;
    private LocalDateTime publishDate;
    private String status;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
