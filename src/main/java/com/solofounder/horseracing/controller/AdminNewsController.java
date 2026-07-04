package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.news.CreateNewsArticleRequest;
import com.solofounder.horseracing.dto.news.NewsArticleResponse;
import com.solofounder.horseracing.dto.news.UpdateNewsArticleRequest;
import com.solofounder.horseracing.service.NewsArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsController {

    private final NewsArticleService newsArticleService;

    @GetMapping
    public ResponseEntity<Page<NewsArticleResponse>> getAllNews(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(newsArticleService.getAdminNews(status, page, size));
    }

    @PostMapping
    public ResponseEntity<NewsArticleResponse> create(@Valid @RequestBody CreateNewsArticleRequest request) {
        return ResponseEntity.ok(newsArticleService.create(request));
    }

    @PutMapping("/{newsId}")
    public ResponseEntity<NewsArticleResponse> update(
            @PathVariable Long newsId,
            @Valid @RequestBody UpdateNewsArticleRequest request) {
        return ResponseEntity.ok(newsArticleService.update(newsId, request));
    }

    @DeleteMapping("/{newsId}")
    public ResponseEntity<NewsArticleResponse> softDelete(@PathVariable Long newsId) {
        return ResponseEntity.ok(newsArticleService.changeStatus(newsId, "deleted"));
    }

    @PutMapping("/{newsId}/publish")
    public ResponseEntity<NewsArticleResponse> publish(@PathVariable Long newsId) {
        return ResponseEntity.ok(newsArticleService.changeStatus(newsId, "published"));
    }

    @PutMapping("/{newsId}/hide")
    public ResponseEntity<NewsArticleResponse> hide(@PathVariable Long newsId) {
        return ResponseEntity.ok(newsArticleService.changeStatus(newsId, "hidden"));
    }
}
