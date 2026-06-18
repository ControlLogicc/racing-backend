package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.racecondition.RaceConditionRequest;
import com.solofounder.horseracing.dto.racecondition.RaceConditionResponse;
import com.solofounder.horseracing.model.RaceCondition;
import com.solofounder.horseracing.repository.RaceConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RaceConditionService {

    private static final Set<String> VALID_TRACK_TYPES = Set.of("turf", "dirt", "synthetic");

    private final RaceConditionRepository raceConditionRepository;

    public List<RaceConditionResponse> getAllRaceConditions() {
        return raceConditionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceConditionResponse getRaceCondition(Long conditionId) {
        return toResponse(findRaceCondition(conditionId));
    }

    public RaceConditionResponse createRaceCondition(RaceConditionRequest request) {
        RaceCondition raceCondition = RaceCondition.builder()
                .conditionName(normalizeRequired(request.getConditionName(), "Race condition name is required"))
                .distance(validateDistance(request.getDistance()))
                .trackType(normalizeTrackType(request.getTrackType()))
                .minEntries(validateMinEntries(request.getMinEntries()))
                .maxEntries(validateMaxEntries(request.getMaxEntries()))
                .classRequirement(trimToNull(request.getClassRequirement()))
                .build();
        validateEntryRange(raceCondition.getMinEntries(), raceCondition.getMaxEntries());
        return toResponse(raceConditionRepository.save(raceCondition));
    }

    public RaceConditionResponse updateRaceCondition(Long conditionId, RaceConditionRequest request) {
        RaceCondition raceCondition = findRaceCondition(conditionId);
        Short minEntries = validateMinEntries(request.getMinEntries());
        Short maxEntries = validateMaxEntries(request.getMaxEntries());
        validateEntryRange(minEntries, maxEntries);
        raceCondition.setConditionName(normalizeRequired(request.getConditionName(), "Race condition name is required"));
        raceCondition.setDistance(validateDistance(request.getDistance()));
        raceCondition.setTrackType(normalizeTrackType(request.getTrackType()));
        raceCondition.setMinEntries(minEntries);
        raceCondition.setMaxEntries(maxEntries);
        raceCondition.setClassRequirement(trimToNull(request.getClassRequirement()));
        return toResponse(raceConditionRepository.save(raceCondition));
    }

    public void deleteRaceCondition(Long conditionId) {
        RaceCondition raceCondition = findRaceCondition(conditionId);
        if (raceConditionRepository.countRaces(conditionId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Race condition is being used by races");
        }
        raceConditionRepository.delete(raceCondition);
    }

    private RaceCondition findRaceCondition(Long conditionId) {
        return raceConditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race condition not found"));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private Integer validateDistance(Integer distance) {
        if (distance == null) {
            throw new IllegalArgumentException("Distance is required");
        }
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be greater than 0");
        }
        return distance;
    }

    private String normalizeTrackType(String trackType) {
        String normalized = trimToNull(trackType);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase();
        if (!VALID_TRACK_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Track type must be turf, dirt, or synthetic");
        }
        return normalized;
    }

    private Short validateMinEntries(Short minEntries) {
        if (minEntries == null) {
            return 3;
        }
        if (minEntries <= 0) {
            throw new IllegalArgumentException("Min entries must be greater than 0");
        }
        return minEntries;
    }

    private Short validateMaxEntries(Short maxEntries) {
        if (maxEntries == null) {
            return 14;
        }
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("Max entries must be greater than 0");
        }
        return maxEntries;
    }

    private void validateEntryRange(Short minEntries, Short maxEntries) {
        if (minEntries > maxEntries) {
            throw new IllegalArgumentException("Min entries must not be greater than max entries");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private RaceConditionResponse toResponse(RaceCondition raceCondition) {
        return RaceConditionResponse.builder()
                .conditionId(raceCondition.getConditionId())
                .conditionName(raceCondition.getConditionName())
                .distance(raceCondition.getDistance())
                .trackType(raceCondition.getTrackType())
                .minEntries(raceCondition.getMinEntries())
                .maxEntries(raceCondition.getMaxEntries())
                .classRequirement(raceCondition.getClassRequirement())
                .createdAt(raceCondition.getCreatedAt())
                .build();
    }
}
