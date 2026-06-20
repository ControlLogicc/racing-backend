package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.racemeeting.RaceMeetingRequest;
import com.solofounder.horseracing.dto.racemeeting.RaceMeetingResponse;
import com.solofounder.horseracing.model.RaceMeeting;
import com.solofounder.horseracing.model.Racecourse;
import com.solofounder.horseracing.model.Season;
import com.solofounder.horseracing.repository.RaceMeetingRepository;
import com.solofounder.horseracing.repository.RacecourseRepository;
import com.solofounder.horseracing.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceMeetingService {

    private static final Set<String> VALID_STATUSES = Set.of("scheduled", "open", "completed", "cancelled");

    private final RaceMeetingRepository raceMeetingRepository;
    private final SeasonRepository seasonRepository;
    private final RacecourseRepository racecourseRepository;

    public List<RaceMeetingResponse> getAllRaceMeetings() {
        return raceMeetingRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceMeetingResponse getRaceMeeting(Long meetingId) {
        return toResponse(findRaceMeeting(meetingId));
    }

    public RaceMeetingResponse createRaceMeeting(RaceMeetingRequest request) {
        Season season = findSeason(request.getSeasonId());
        Racecourse racecourse = findRacecourse(request.getRacecourseId());
        validateMeetingDate(request.getMeetingDate(), season);
        RaceMeeting raceMeeting = RaceMeeting.builder()
                .season(season)
                .racecourse(racecourse)
                .meetingDate(request.getMeetingDate())
                .meetingName(request.getMeetingName())
                .status("scheduled")
                .build();
        return toResponse(raceMeetingRepository.save(raceMeeting));
    }

    public RaceMeetingResponse updateRaceMeeting(Long meetingId, RaceMeetingRequest request) {
        RaceMeeting raceMeeting = findRaceMeeting(meetingId);
        Season season = findSeason(request.getSeasonId());
        Racecourse racecourse = findRacecourse(request.getRacecourseId());
        validateMeetingDate(request.getMeetingDate(), season);
        raceMeeting.setSeason(season);
        raceMeeting.setRacecourse(racecourse);
        raceMeeting.setMeetingDate(request.getMeetingDate());
        raceMeeting.setMeetingName(request.getMeetingName());
        return toResponse(raceMeetingRepository.save(raceMeeting));
    }

    public void deleteRaceMeeting(Long meetingId) {
        RaceMeeting raceMeeting = findRaceMeeting(meetingId);
        if (raceMeetingRepository.countRaces(meetingId) > 0) {
            throw new IllegalStateException("Cannot delete race meeting with existing race");
        }
        raceMeetingRepository.delete(raceMeeting);
    }

    private RaceMeeting findRaceMeeting(Long meetingId) {
        return raceMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race meeting not found"));
    }

    private Season findSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found"));
    }

    private Racecourse findRacecourse(Long racecourseId) {
        return racecourseRepository.findById(racecourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Racecourse not found"));
    }

    private void validateMeetingDate(LocalDate meetingDate, Season season) {
        if (meetingDate == null) {
            throw new IllegalArgumentException("Meeting date is required");
        }
        if (meetingDate.isBefore(season.getStartDate()) || meetingDate.isAfter(season.getEndDate())) {
            throw new IllegalArgumentException("Meeting date must be within season start date and end date");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "scheduled";
        }
        normalized = normalized.toLowerCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Race meeting status must be scheduled, open, completed, or cancelled");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private RaceMeetingResponse toResponse(RaceMeeting raceMeeting) {
        Season season = raceMeeting.getSeason();
        Racecourse racecourse = raceMeeting.getRacecourse();
        return RaceMeetingResponse.builder()
                .meetingId(raceMeeting.getMeetingId())
                .meetingName(raceMeeting.getMeetingName())
                .seasonId(season.getSeasonId())
                .seasonName(season.getSeasonName())
                .racecourseId(racecourse.getRacecourseId())
                .racecourseName(racecourse.getRacecourseName())
                .meetingDate(raceMeeting.getMeetingDate())
                .createdAt(raceMeeting.getCreatedAt())
                .build();
    }
}
