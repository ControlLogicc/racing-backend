package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.news.CreateNewsArticleRequest;
import com.solofounder.horseracing.dto.news.NewsArticleResponse;
import com.solofounder.horseracing.dto.news.UpdateNewsArticleRequest;
import com.solofounder.horseracing.model.NewsArticle;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.NewsArticleRepository;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NewsArticleService {

    private static final Set<String> VALID_STATUSES =
            Set.of("draft", "published", "hidden", "deleted");

    private final NewsArticleRepository newsArticleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NewsArticleResponse> getPublishedNews(int page, int size) {
        return newsArticleRepository.findByStatus("published", pageable(page, size))
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public NewsArticleResponse getPublishedNewsById(Long newsId) {
        NewsArticle article = newsArticleRepository.findByNewsIdAndStatus(newsId, "published")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found"));
        return toResponse(article);
    }

    @Transactional(readOnly = true)
    public Page<NewsArticleResponse> getAdminNews(String status, int page, int size) {
        Pageable pageable = pageable(page, size);
        Page<NewsArticle> articles = status == null || status.isBlank()
                ? newsArticleRepository.findAll(pageable)
                : newsArticleRepository.findByStatus(normalizeStatus(status), pageable);
        return articles.map(this::toResponse);
    }

    @Transactional
    public NewsArticleResponse create(CreateNewsArticleRequest request) {
        User admin = getCurrentAdmin();
        NewsArticle article = NewsArticle.builder()
                .title(request.getTitle().trim())
                .summary(trimToNull(request.getSummary()))
                .content(trimToNull(request.getContent()))
                .thumbnailUrl(trimToNull(request.getThumbnailUrl()))
                .externalLink(trimToNull(request.getExternalLink()))
                .publishDate(request.getPublishDate())
                .status(request.getStatus() == null ? "published" : normalizeStatus(request.getStatus()))
                .createdBy(admin.getUserId())
                .build();
        return toResponse(newsArticleRepository.save(article));
    }

    @Transactional
    public NewsArticleResponse update(Long newsId, UpdateNewsArticleRequest request) {
        User admin = getCurrentAdmin();
        NewsArticle article = findRequired(newsId);
        article.setTitle(request.getTitle().trim());
        article.setSummary(trimToNull(request.getSummary()));
        article.setContent(trimToNull(request.getContent()));
        article.setThumbnailUrl(trimToNull(request.getThumbnailUrl()));
        article.setExternalLink(trimToNull(request.getExternalLink()));
        article.setPublishDate(request.getPublishDate() == null
                ? article.getPublishDate()
                : request.getPublishDate());
        if (request.getStatus() != null) {
            article.setStatus(normalizeStatus(request.getStatus()));
        }
        markUpdated(article, admin.getUserId());
        return toResponse(newsArticleRepository.save(article));
    }

    @Transactional
    public NewsArticleResponse changeStatus(Long newsId, String status) {
        User admin = getCurrentAdmin();
        NewsArticle article = findRequired(newsId);
        article.setStatus(normalizeStatus(status));
        markUpdated(article, admin.getUserId());
        return toResponse(newsArticleRepository.save(article));
    }

    private NewsArticle findRequired(Long newsId) {
        return newsArticleRepository.findById(newsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found"));
    }

    private User getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return user;
    }

    private Pageable pageable(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishDate"));
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be draft, published, hidden, or deleted"
            );
        }
        return normalized;
    }

    private void markUpdated(NewsArticle article, Long adminId) {
        article.setUpdatedBy(adminId);
        article.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private NewsArticleResponse toResponse(NewsArticle article) {
        return NewsArticleResponse.builder()
                .newsId(article.getNewsId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .content(article.getContent())
                .thumbnailUrl(article.getThumbnailUrl())
                .externalLink(article.getExternalLink())
                .publishDate(article.getPublishDate())
                .status(article.getStatus())
                .createdBy(article.getCreatedBy())
                .updatedBy(article.getUpdatedBy())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private NewsArticleResponse toSummaryResponse(NewsArticle article) {
        return NewsArticleResponse.builder()
                .newsId(article.getNewsId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .thumbnailUrl(article.getThumbnailUrl())
                .externalLink(article.getExternalLink())
                .publishDate(article.getPublishDate())
                .status(article.getStatus())
                .build();
    }
}
