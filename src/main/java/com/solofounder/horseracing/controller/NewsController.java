package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.news.NewsArticleResponse;
import com.solofounder.horseracing.service.NewsArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsArticleService newsArticleService;

    @GetMapping
    public ResponseEntity<Page<NewsArticleResponse>> getPublishedNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(newsArticleService.getPublishedNews(page, size));
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<NewsArticleResponse> getPublishedNewsById(@PathVariable Long newsId) {
        return ResponseEntity.ok(newsArticleService.getPublishedNewsById(newsId));
    }
}
