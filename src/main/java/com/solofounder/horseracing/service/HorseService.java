package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.horse.CreateHorseRequest;
import com.solofounder.horseracing.dto.horse.UpdateHorseRequest;
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
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INJURED", "RETIRED", "SUSPENDED");

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

    public HorseResponse createOwnerHorse(CreateHorseRequest request) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        Horse horse = Horse.builder()
                .owner(owner)
                .horseName(normalizeRequiredName(request.getHorseName()))
                .color(normalizeRequired(request.getColor(), "Color is required"))
                .age(validateAge(request.getAge()))
                .gender(normalizeGender(request.getGender()))
                .currentScore(BigDecimal.ZERO)
                .horseClass((short) 5) // Default to Class 5
                .healthNote(trimToNull(request.getHealthNote()))
                .status("active") // Default to ACTIVE (stored as active)
                .build();
        return toResponse(horseRepository.save(horse));
    }

    public HorseResponse updateOwnerHorse(Long horseId, UpdateHorseRequest request) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        Horse horse = horseRepository.findByHorseIdAndOwnerUserId(horseId, owner.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        
        horse.setHorseName(normalizeRequiredName(request.getHorseName()));
        horse.setColor(normalizeRequired(request.getColor(), "Color is required"));
        horse.setAge(validateAge(request.getAge()));
        horse.setGender(normalizeGender(request.getGender()));
        horse.setHealthNote(trimToNull(request.getHealthNote()));
        
        // Translate status uppercase value from request to lowercase value for database
        horse.setStatus(normalizeHorseStatus(request.getStatus()));
        
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

    public HorseResponse updateHorse(Long horseId, UpdateHorseRequest request) {
        Horse horse = findHorse(horseId);
        horse.setHorseName(normalizeRequiredName(request.getHorseName()));
        horse.setColor(normalizeRequired(request.getColor(), "Color is required"));
        horse.setAge(validateAge(request.getAge()));
        horse.setGender(normalizeGender(request.getGender()));
        horse.setHealthNote(trimToNull(request.getHealthNote()));
        horse.setStatus(normalizeHorseStatus(request.getStatus()));
        return toResponse(horseRepository.save(horse));
    }

    public void deleteHorseByAdmin(Long horseId) {
        deleteHorse(findHorse(horseId));
    }

    // Classification helper method mapping score to horse class
    public Short calculateHorseClass(BigDecimal currentScore) {
        if (currentScore == null || currentScore.compareTo(BigDecimal.ZERO) < 0) {
            return 5;
        }

        if (currentScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return 1;
        }

        if (currentScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return 2;
        }

        if (currentScore.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return 3;
        }

        if (currentScore.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return 4;
        }

        return 5;
    }

    private Horse findHorse(Long horseId) {
        return horseRepository.findById(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
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

    private String normalizeRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private Short validateAge(Short age) {
        if (age == null || age < 1 || age > 30) {
            throw new IllegalArgumentException("Horse age must be between 1 and 30");
        }
        return age;
    }

    private String normalizeGender(String gender) {
        String normalized = trimToNull(gender);
        if (normalized == null) {
            throw new IllegalArgumentException("Gender is required");
        }
        normalized = normalized.toUpperCase();
        if (!VALID_GENDERS.contains(normalized)) {
            throw new IllegalArgumentException("Horse gender must be M or F");
        }
        return normalized;
    }

    private String normalizeHorseStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "active";
        }
        normalized = normalized.toUpperCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Horse status must be ACTIVE, INJURED, RETIRED, or SUSPENDED");
        }
        return normalized.toLowerCase();
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
                .color(horse.getColor())
                .age(horse.getAge())
                .gender(horse.getGender())
                .currentScore(horse.getCurrentScore())
                .horseClass(horse.getHorseClass())
                .healthNote(horse.getHealthNote())
                .status(horse.getStatus() != null ? horse.getStatus().toUpperCase() : "ACTIVE") // Return status as uppercase
                .build();
    }
}
