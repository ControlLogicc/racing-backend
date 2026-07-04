package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.news.CreateNewsArticleRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NewsArticleValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void blankTitleIsRejected() {
        CreateNewsArticleRequest request = CreateNewsArticleRequest.builder()
                .title(" ")
                .build();

        Set<ConstraintViolation<CreateNewsArticleRequest>> violations = validator.validate(request);

        assertEquals("Title is required", violations.iterator().next().getMessage());
    }

    @Test
    void invalidExternalLinkIsRejected() {
        CreateNewsArticleRequest request = CreateNewsArticleRequest.builder()
                .title("Valid title")
                .externalLink("not-a-url")
                .build();

        Set<ConstraintViolation<CreateNewsArticleRequest>> violations = validator.validate(request);

        assertEquals("External link must be a valid HTTP URL", violations.iterator().next().getMessage());
    }
}
