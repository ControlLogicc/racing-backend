package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.horse.HorseRequest;
import com.solofounder.horseracing.dto.horse.HorseResponse;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class HorseService {

    private static final Set<String> VALID_GENDERS = Set.of("M", "F");
    private static final Set<String> VALID_STATUSES = Set.of("active", "injured", "retired", "suspended");

    private final HorseRepository horseRepository;
    private final UserRepository userRepository;

    public List<HorseResponse> getOwnerHorses() {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        return horseRepository.findByOwnerUserId(owner.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public HorseResponse getOwnerHorse(Long horseId) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        Horse horse = horseRepository.findByHorseIdAndOwnerUserId(horseId, owner.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        return toResponse(horse);
    }

    public HorseResponse createOwnerHorse(HorseRequest request) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        Horse horse = Horse.builder()
                .owner(owner)
                .horseName(normalizeRequiredName(request.getHorseName()))
                .age(validateAge(request.getAge()))
                .gender(normalizeGender(request.getGender()))
                .breed(trimToNull(request.getBreed()))
                .currentScore(validateCurrentScore(request.getCurrentScore()))
                .horseClass(validateHorseClass(request.getHorseClass()))
                .status(normalizeHorseStatus(request.getStatus()))
                .build();
        return toResponse(horseRepository.save(horse));
    }

    public HorseResponse updateOwnerHorse(Long horseId, HorseRequest request) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        Horse horse = horseRepository.findByHorseIdAndOwnerUserId(horseId, owner.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        updateHorseFields(horse, request);
        return toResponse(horseRepository.save(horse));
    }

    public void deleteOwnerHorse(Long horseId) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        Horse horse = horseRepository.findByHorseIdAndOwnerUserId(horseId, owner.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        deleteHorse(horse);
    }

    public List<HorseResponse> getAllHorses() {
        return horseRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public HorseResponse getHorse(Long horseId) {
        return toResponse(findHorse(horseId));
    }

    public HorseResponse updateHorse(Long horseId, HorseRequest request) {
        Horse horse = findHorse(horseId);
        updateHorseFields(horse, request);
        return toResponse(horseRepository.save(horse));
    }

    public void deleteHorseByAdmin(Long horseId) {
        deleteHorse(findHorse(horseId));
    }

    private Horse findHorse(Long horseId) {
        return horseRepository.findById(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
    }

    private void updateHorseFields(Horse horse, HorseRequest request) {
        horse.setHorseName(normalizeRequiredName(request.getHorseName()));
        horse.setAge(validateAge(request.getAge()));
        horse.setGender(normalizeGender(request.getGender()));
        horse.setBreed(trimToNull(request.getBreed()));
        horse.setCurrentScore(validateCurrentScore(request.getCurrentScore()));
        horse.setHorseClass(validateHorseClass(request.getHorseClass()));
        horse.setStatus(normalizeHorseStatus(request.getStatus()));
    }

    private void deleteHorse(Horse horse) {
        if (horseRepository.countRaceRegistrations(horse.getHorseId()) > 0
                || horseRepository.countRaceEntries(horse.getHorseId()) > 0) {
            throw new IllegalStateException("Cannot delete horse with existing race registration or race entry");
        }
        horseRepository.delete(horse);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private void requireRole(User user, Role role) {
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private String normalizeRequiredName(String value) {
        String name = trimToNull(value);
        if (name == null) {
            throw new IllegalArgumentException("Horse name is required");
        }
        return name;
    }

    private Short validateAge(Short age) {
        if (age != null && age < 0) {
            throw new IllegalArgumentException("Horse age must be greater than or equal to 0");
        }
        return age;
    }

    private String normalizeGender(String gender) {
        String normalized = trimToNull(gender);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase();
        if (!VALID_GENDERS.contains(normalized)) {
            throw new IllegalArgumentException("Horse gender must be M or F");
        }
        return normalized;
    }

    private BigDecimal validateCurrentScore(BigDecimal currentScore) {
        if (currentScore != null && currentScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current score must be greater than or equal to 0");
        }
        return currentScore == null ? BigDecimal.ZERO : currentScore;
    }

    private Short validateHorseClass(Short horseClass) {
        if (horseClass == null) {
            return 5;
        }
        if (horseClass < 1 || horseClass > 5) {
            throw new IllegalArgumentException("Horse class must be between 1 and 5");
        }
        return horseClass;
    }

    private String normalizeHorseStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "active";
        }
        normalized = normalized.toLowerCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Horse status must be active, injured, retired, or suspended");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private HorseResponse toResponse(Horse horse) {
        User owner = horse.getOwner();
        return HorseResponse.builder()
                .horseId(horse.getHorseId())
                .ownerId(owner.getUserId())
                .ownerName(owner.getFullName())
                .horseName(horse.getHorseName())
                .age(horse.getAge())
                .gender(horse.getGender())
                .breed(horse.getBreed())
                .currentScore(horse.getCurrentScore())
                .horseClass(horse.getHorseClass())
                .status(horse.getStatus())
                .createdAt(horse.getCreatedAt())
                .updatedAt(horse.getUpdatedAt())
                .build();
    }
}
