package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.news.CreateNewsArticleRequest;
import com.solofounder.horseracing.dto.news.NewsArticleResponse;
import com.solofounder.horseracing.dto.news.UpdateNewsArticleRequest;
import com.solofounder.horseracing.model.NewsArticle;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.NewsArticleRepository;
import com.solofounder.horseracing.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsArticleServiceTests {

    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NewsArticleService newsArticleService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .userId(1L)
                .email("admin@example.com")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        authenticate(admin);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicListQueriesOnlyPublishedAndOmitsContent() {
        NewsArticle article = article("published");
        article.setContent("Detail content");
        when(newsArticleRepository.findByStatus(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article)));

        Page<NewsArticleResponse> response = newsArticleService.getPublishedNews(0, 10);

        assertEquals(1, response.getTotalElements());
        assertEquals("published", response.getContent().get(0).getStatus());
        assertNull(response.getContent().get(0).getContent());
        verify(newsArticleRepository).findByStatus(any(), any(Pageable.class));
    }

    @Test
    void publicDetailDoesNotReturnHiddenArticle() {
        when(newsArticleRepository.findByNewsIdAndStatus(5L, "published"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> newsArticleService.getPublishedNewsById(5L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void adminCanCreateArticle() {
        mockAdmin();
        when(newsArticleRepository.save(any(NewsArticle.class))).thenAnswer(invocation -> {
            NewsArticle article = invocation.getArgument(0);
            article.setNewsId(10L);
            return article;
        });

        NewsArticleResponse response = newsArticleService.create(CreateNewsArticleRequest.builder()
                .title(" Racing News ")
                .summary("Summary")
                .externalLink("https://example.com/news")
                .status("PUBLISHED")
                .build());

        assertEquals(10L, response.getNewsId());
        assertEquals("Racing News", response.getTitle());
        assertEquals("published", response.getStatus());
        assertEquals(admin.getUserId(), response.getCreatedBy());
    }

    @Test
    void adminCanUpdateArticle() {
        mockAdmin();
        NewsArticle article = article("draft");
        when(newsArticleRepository.findById(article.getNewsId())).thenReturn(Optional.of(article));
        when(newsArticleRepository.save(article)).thenReturn(article);

        NewsArticleResponse response = newsArticleService.update(article.getNewsId(),
                UpdateNewsArticleRequest.builder()
                        .title("Updated title")
                        .status("hidden")
                        .build());

        assertEquals("Updated title", response.getTitle());
        assertEquals("hidden", response.getStatus());
        assertEquals(admin.getUserId(), response.getUpdatedBy());
    }

    @Test
    void adminSoftDeleteSetsDeletedStatus() {
        mockAdmin();
        NewsArticle article = article("published");
        when(newsArticleRepository.findById(article.getNewsId())).thenReturn(Optional.of(article));
        when(newsArticleRepository.save(article)).thenReturn(article);

        NewsArticleResponse response = newsArticleService.changeStatus(article.getNewsId(), "deleted");

        assertEquals("deleted", response.getStatus());
        verify(newsArticleRepository, never()).delete(any());
    }

    @Test
    void nonAdminCannotCreateArticle() {
        User owner = User.builder()
                .userId(2L)
                .email("owner@example.com")
                .role(Role.OWNER)
                .status(UserStatus.ACTIVE)
                .build();
        authenticate(owner);
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> newsArticleService.create(CreateNewsArticleRequest.builder()
                        .title("Forbidden news")
                        .build())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(newsArticleRepository, never()).save(any());
    }

    private NewsArticle article(String status) {
        return NewsArticle.builder()
                .newsId(5L)
                .title("Race update")
                .summary("Summary")
                .publishDate(LocalDateTime.now())
                .status(status)
                .createdBy(1L)
                .build();
    }

    private void mockAdmin() {
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );
    }
}
