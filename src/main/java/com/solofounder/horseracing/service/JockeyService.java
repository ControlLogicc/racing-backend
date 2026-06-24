package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.jockey.CreateJockeyRequest;
import com.solofounder.horseracing.dto.jockey.JockeyResponse;
import com.solofounder.horseracing.dto.jockey.UpdateJockeyRequest;
import com.solofounder.horseracing.dto.jockey.UpdateJockeyWeightRequest;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.JockeyRepository;
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
public class JockeyService {

    private static final Set<String> VALID_STATUSES = Set.of("available", "unavailable", "suspended");

    private final JockeyRepository jockeyRepository;
    private final UserRepository userRepository;
    private final RaceService raceService;

    public List<JockeyResponse> getAllJockeys() {
        return jockeyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public JockeyResponse getJockey(Long jockeyId) {
        return toResponse(findJockey(jockeyId));
    }

    public JockeyResponse createJockey(CreateJockeyRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.JOCKEY) {
            throw new IllegalArgumentException("User role must be JOCKEY");
        }
        if (jockeyRepository.existsByUserUserId(user.getUserId())) {
            throw new IllegalStateException("Jockey profile already exists for this user");
        }

        Jockey jockey = Jockey.builder()
                .user(user)
                .weight(validateWeight(request.getWeight()))
                .experienceYears(validateExperienceYears(request.getExperienceYears()))
                .status(normalizeJockeyStatus(request.getStatus()))
                .build();
        return toResponse(jockeyRepository.save(jockey));
    }

    public JockeyResponse updateJockey(Long jockeyId, UpdateJockeyRequest request) {
        Jockey jockey = findJockey(jockeyId);
        updateJockeyFields(jockey, request);
        return toResponse(jockeyRepository.save(jockey));
    }

    public void deleteJockey(Long jockeyId) {
        Jockey jockey = findJockey(jockeyId);
        if (jockeyRepository.countRaceInvitations(jockeyId) > 0
                || jockeyRepository.countRaceEntries(jockeyId) > 0) {
            throw new IllegalStateException("Cannot delete jockey with existing race invitation or race entry");
        }
        jockeyRepository.delete(jockey);
    }

    public JockeyResponse getCurrentJockeyProfile() {
        User user = getCurrentUser();
        requireRole(user, Role.JOCKEY);
        Jockey jockey = jockeyRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> jockeyRepository.save(
                        Jockey.builder()
                                .user(user)
                                .status("available")
                                .build()
                ));
        return toResponse(jockey);
    }

    public JockeyResponse updateCurrentJockeyProfile(UpdateJockeyRequest request) {
        User user = getCurrentUser();
        requireRole(user, Role.JOCKEY);
        Jockey jockey = jockeyRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> jockeyRepository.save(
                        Jockey.builder()
                                .user(user)
                                .status("available")
                                .build()
                ));
        updateJockeyFields(jockey, request);
        return toResponse(jockeyRepository.save(jockey));
    }

    public JockeyResponse updateOwnWeight(Long jockeyId, UpdateJockeyWeightRequest request) {
        Jockey targetJockey = findJockey(jockeyId);
        User user = getCurrentUser();
        requireRole(user, Role.JOCKEY);
        Jockey currentJockey = jockeyRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> jockeyRepository.save(
                        Jockey.builder()
                                .user(user)
                                .status("available")
                                .build()
                ));

        if (!targetJockey.getJockeyId().equals(currentJockey.getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        targetJockey.setWeight(validateRequiredWeight(request.getWeight()));
        return toResponse(jockeyRepository.save(targetJockey));
    }

    public List<RaceResponse> getAvailableRacesForCurrentJockey() {
        User user = getCurrentUser();
        requireRole(user, Role.JOCKEY);
        jockeyRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));
        return raceService.getOpenRaces();
    }

    private Jockey findJockey(Long jockeyId) {
        return jockeyRepository.findById(jockeyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey not found"));
    }

    private void updateJockeyFields(Jockey jockey, UpdateJockeyRequest request) {
        jockey.setWeight(validateWeight(request.getWeight()));
        jockey.setExperienceYears(validateExperienceYears(request.getExperienceYears()));
        jockey.setStatus(normalizeJockeyStatus(request.getStatus()));
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

    private BigDecimal validateWeight(BigDecimal weight) {
        if (weight != null && weight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weight must be greater than or equal to 0");
        }
        return weight;
    }

    private BigDecimal validateRequiredWeight(BigDecimal weight) {
        if (weight == null) {
            throw new IllegalArgumentException("Weight is required");
        }
        if (weight.compareTo(BigDecimal.valueOf(30)) < 0 || weight.compareTo(BigDecimal.valueOf(80)) > 0) {
            throw new IllegalArgumentException("Weight must be between 30 and 80");
        }
        return weight;
    }

    private Short validateExperienceYears(Short experienceYears) {
        if (experienceYears != null && experienceYears < 0) {
            throw new IllegalArgumentException("Experience years must be greater than or equal to 0");
        }
        return experienceYears;
    }

    private String normalizeJockeyStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "available";
        }
        normalized = normalized.toLowerCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Jockey status must be available, unavailable, or suspended");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private JockeyResponse toResponse(Jockey jockey) {
        User user = jockey.getUser();
        return JockeyResponse.builder()
                .jockeyId(jockey.getJockeyId())
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .weight(jockey.getWeight())
                .experienceYears(jockey.getExperienceYears())
                .status(jockey.getStatus())
                .createdAt(jockey.getCreatedAt())
                .build();
    }
}
