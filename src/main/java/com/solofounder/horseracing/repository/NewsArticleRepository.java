package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    Page<NewsArticle> findByStatus(String status, Pageable pageable);
    Optional<NewsArticle> findByNewsIdAndStatus(Long newsId, String status);
}
