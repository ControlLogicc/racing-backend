package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.season.SeasonRequest;
import com.solofounder.horseracing.dto.season.SeasonResponse;
import com.solofounder.horseracing.model.Season;
import com.solofounder.horseracing.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private static final Set<String> VALID_STATUSES = Set.of("draft", "active", "completed", "cancelled");

    private final SeasonRepository seasonRepository;

    public List<SeasonResponse> getAllSeasons() {
        return seasonRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public SeasonResponse getSeason(Long seasonId) {
        return toResponse(findSeason(seasonId));
    }

    public SeasonResponse createSeason(SeasonRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        Season season = Season.builder()
                .seasonName(normalizeRequired(request.getSeasonName(), "Season name is required"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(normalizeStatus(request.getStatus()))
                .build();
        return toResponse(seasonRepository.save(season));
    }

    public SeasonResponse updateSeason(Long seasonId, SeasonRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        Season season = findSeason(seasonId);
        season.setSeasonName(normalizeRequired(request.getSeasonName(), "Season name is required"));
        season.setStartDate(request.getStartDate());
        season.setEndDate(request.getEndDate());
        season.setStatus(normalizeStatus(request.getStatus()));
        return toResponse(seasonRepository.save(season));
    }

    public void deleteSeason(Long seasonId) {
        Season season = findSeason(seasonId);
        if (seasonRepository.countRaceMeetings(seasonId) > 0) {
            throw new IllegalStateException("Cannot delete season with existing race meeting");
        }
        seasonRepository.delete(season);
    }

    private Season findSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found"));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "draft";
        }
        normalized = normalized.toLowerCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Season status must be draft, active, completed, or cancelled");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private SeasonResponse toResponse(Season season) {
        return SeasonResponse.builder()
                .seasonId(season.getSeasonId())
                .seasonName(season.getSeasonName())
                .startDate(season.getStartDate())
                .endDate(season.getEndDate())
                .status(season.getStatus() != null ? season.getStatus().toUpperCase() : "DRAFT")
                .createdAt(season.getCreatedAt())
                .build();
    }
}
