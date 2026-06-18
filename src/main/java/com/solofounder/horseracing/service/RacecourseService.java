package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.racecourse.RacecourseRequest;
import com.solofounder.horseracing.dto.racecourse.RacecourseResponse;
import com.solofounder.horseracing.model.Racecourse;
import com.solofounder.horseracing.repository.RacecourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RacecourseService {

    private static final Set<String> VALID_SURFACE_TYPES = Set.of("turf", "dirt", "synthetic");

    private final RacecourseRepository racecourseRepository;

    public List<RacecourseResponse> getAllRacecourses() {
        return racecourseRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public RacecourseResponse getRacecourse(Long racecourseId) {
        return toResponse(findRacecourse(racecourseId));
    }

    public RacecourseResponse createRacecourse(RacecourseRequest request) {
        Racecourse racecourse = Racecourse.builder()
                .racecourseName(normalizeRequired(request.getRacecourseName(), "Racecourse name is required"))
                .location(trimToNull(request.getLocation()))
                .surfaceType(normalizeSurfaceType(request.getSurfaceType()))
                .capacity(validateCapacity(request.getCapacity()))
                .build();
        return toResponse(racecourseRepository.save(racecourse));
    }

    public RacecourseResponse updateRacecourse(Long racecourseId, RacecourseRequest request) {
        Racecourse racecourse = findRacecourse(racecourseId);
        racecourse.setRacecourseName(normalizeRequired(request.getRacecourseName(), "Racecourse name is required"));
        racecourse.setLocation(trimToNull(request.getLocation()));
        racecourse.setSurfaceType(normalizeSurfaceType(request.getSurfaceType()));
        racecourse.setCapacity(validateCapacity(request.getCapacity()));
        return toResponse(racecourseRepository.save(racecourse));
    }

    public void deleteRacecourse(Long racecourseId) {
        Racecourse racecourse = findRacecourse(racecourseId);
        if (racecourseRepository.countRaceMeetings(racecourseId) > 0) {
            throw new IllegalStateException("Cannot delete racecourse with existing race meeting");
        }
        racecourseRepository.delete(racecourse);
    }

    private Racecourse findRacecourse(Long racecourseId) {
        return racecourseRepository.findById(racecourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Racecourse not found"));
    }

    private String normalizeSurfaceType(String surfaceType) {
        String normalized = trimToNull(surfaceType);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase();
        if (!VALID_SURFACE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Surface type must be turf, dirt, or synthetic");
        }
        return normalized;
    }

    private Integer validateCapacity(Integer capacity) {
        if (capacity != null && capacity < 0) {
            throw new IllegalArgumentException("Capacity must be greater than or equal to 0");
        }
        return capacity;
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

    private RacecourseResponse toResponse(Racecourse racecourse) {
        return RacecourseResponse.builder()
                .racecourseId(racecourse.getRacecourseId())
                .racecourseName(racecourse.getRacecourseName())
                .location(racecourse.getLocation())
                .surfaceType(racecourse.getSurfaceType())
                .capacity(racecourse.getCapacity())
                .createdAt(racecourse.getCreatedAt())
                .build();
    }
}
