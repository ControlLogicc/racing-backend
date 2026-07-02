package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.horse.CreateHorseRequest;
import com.solofounder.horseracing.dto.horse.UpdateHorseRequest;
import com.solofounder.horseracing.dto.horse.HorseResponse;
import com.solofounder.horseracing.dto.horse.VerifyHorseRatingRequest;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.enums.HorseRegistrationType;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class HorseService {

    private static final Set<String> VALID_GENDERS = Set.of("M", "F");
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INJURED", "RETIRED", "SUSPENDED", "FAIL");

    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

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

        // Parse registration type (default NEW)
        HorseRegistrationType regType = parseRegistrationType(request.getRegistrationType());

        // Validate claimed fields for PREVIOUSLY_REGISTERED
        if (regType == HorseRegistrationType.PREVIOUSLY_REGISTERED) {
            if (request.getClaimedScore() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "claimedScore is required for PREVIOUSLY_REGISTERED horses");
            }
        }

        BigDecimal initialScore = (regType == HorseRegistrationType.NEW) ? BigDecimal.valueOf(50) : BigDecimal.ZERO;
        Short initialClass = (regType == HorseRegistrationType.NEW) ? calculateHorseClass(initialScore) : (short) 5;

        Horse horse = Horse.builder()
                .owner(owner)
                .horseName(normalizeRequiredName(request.getHorseName()))
                .color(normalizeRequired(request.getColor(), "Color is required"))
                .age(validateAge(request.getAge()))
                .gender(normalizeGender(request.getGender()))
                .currentScore(initialScore)
                .horseClass(initialClass)
                .totalWins(0)
                .healthNote(trimToNull(request.getHealthNote()))
                .status(regType == HorseRegistrationType.NEW ? "active" : "fail")
                .registrationType(regType)
                .claimedScore(regType == HorseRegistrationType.NEW ? BigDecimal.valueOf(50) : request.getClaimedScore())
                .claimedClass(regType == HorseRegistrationType.NEW ? (short) 3 : calculateHorseClass(request.getClaimedScore()))
                .evidenceLink(regType == HorseRegistrationType.PREVIOUSLY_REGISTERED ? request.getEvidenceLink() : null)
                // NEW = auto-verified; PREVIOUSLY_REGISTERED = pending Staff review
                .ratingVerified(regType == HorseRegistrationType.NEW)
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
        if (!horse.isRatingVerified()) {
            horse.setStatus("fail");
        } else {
            horse.setStatus(normalizeHorseStatus(request.getStatus()));
        }
        
        return toResponse(horseRepository.save(horse));
    }

    public void deleteOwnerHorse(Long horseId) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);
        Horse horse = horseRepository.findByHorseIdAndOwnerUserId(horseId, owner.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));
        deleteHorse(horse);
    }

    // ── Staff / Admin: rating verification ───────────────────────────────────

    /**
     * Verify (approve) the claimed rating of a PREVIOUSLY_REGISTERED horse.
     * Staff/Admin can optionally override the Owner's claimed score and class.
     */
    public HorseResponse verifyHorseRating(Long horseId, VerifyHorseRatingRequest request) {
        User currentUser = getCurrentUser();
        requireStaffOrAdmin(currentUser);

        Horse horse = findHorse(horseId);

        if (horse.getRegistrationType() != HorseRegistrationType.PREVIOUSLY_REGISTERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PREVIOUSLY_REGISTERED horses require rating verification");
        }
        if (horse.isRatingVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Horse rating has already been verified");
        }

        // Resolve final score and class: staff override takes priority over owner's claim
        BigDecimal finalScore = request != null && request.getApprovedScore() != null
                ? request.getApprovedScore()
                : horse.getClaimedScore();
        Short finalClass = request != null && request.getApprovedClass() != null
                ? request.getApprovedClass()
                : horse.getClaimedClass();

        if (finalScore == null || finalClass == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "approvedScore and approvedClass are required (no claimed values found)");
        }

        horse.setClaimedScore(finalScore);
        if (finalClass != null) {
            horse.setClaimedClass(finalClass);
        }
        horse.setCurrentScore(finalScore);
        horse.setHorseClass(finalClass);
        horse.setRatingVerified(true);
        horse.setRatingVerifiedBy(resolveStaffId(currentUser));
        horse.setRatingVerifiedAt(LocalDateTime.now());
        horse.setStatus("active");

        return toResponse(horseRepository.save(horse));
    }

    /**
     * Reject the claimed rating of a PREVIOUSLY_REGISTERED horse.
     * The horse is NOT deleted. It keeps default class 5, score 0.
     * Owner may update the claim and resubmit (or Staff can re-verify).
     */
    public HorseResponse rejectHorseRating(Long horseId) {
        User currentUser = getCurrentUser();
        requireStaffOrAdmin(currentUser);

        Horse horse = findHorse(horseId);

        if (horse.getRegistrationType() != HorseRegistrationType.PREVIOUSLY_REGISTERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PREVIOUSLY_REGISTERED horses require rating verification");
        }
        if (horse.isRatingVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Horse rating has already been verified");
        }

        // Clear the claimed values, keep defaults (score=0, class=5)
        horse.setClaimedScore(null);
        horse.setClaimedClass(null);
        horse.setCurrentScore(BigDecimal.ZERO);
        horse.setHorseClass((short) 5);
        // ratingVerified stays false — owner must re-submit claim
        horse.setRatingVerifiedBy(resolveStaffId(currentUser));
        horse.setRatingVerifiedAt(LocalDateTime.now());
        horse.setStatus("fail");

        return toResponse(horseRepository.save(horse));
    }

    /**
     * Returns all PREVIOUSLY_REGISTERED horses whose rating has NOT yet been verified.
     * Intended for Staff to review and approve/reject owner-submitted ratings.
     */
    public List<HorseResponse> getPendingHorses() {
        return horseRepository
                .findByRegistrationTypeAndRatingVerifiedFalse(HorseRegistrationType.PREVIOUSLY_REGISTERED)
                .stream()
                .map(this::toResponse)
                .toList();
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

    private void requireStaffOrAdmin(User user) {
        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private HorseRegistrationType parseRegistrationType(String raw) {
        if (raw == null || raw.isBlank()) {
            return HorseRegistrationType.NEW;
        }
        try {
            return HorseRegistrationType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "registrationType must be NEW or PREVIOUSLY_REGISTERED");
        }
    }

    /** Returns staffId if the user is a Staff, otherwise null (for ADMIN). */
    private Long resolveStaffId(User user) {
        if (user.getRole() == Role.STAFF) {
            return staffRepository.findByUserUserId(user.getUserId())
                    .map(staff -> staff.getStaffId())
                    .orElse(null);
        }
        return null;
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
                .totalWins(horse.getTotalWins())
                .healthNote(horse.getHealthNote())
                .status(horse.getStatus() != null ? horse.getStatus().toUpperCase() : "ACTIVE")
                .registrationType(horse.getRegistrationType() != null
                        ? horse.getRegistrationType().name() : HorseRegistrationType.NEW.name())
                .claimedScore(horse.getClaimedScore())
                .claimedClass(horse.getClaimedClass())
                .evidenceLink(horse.getEvidenceLink())
                .ratingVerified(horse.isRatingVerified())
                .ratingVerifiedAt(horse.getRatingVerifiedAt())
                .build();
    }
}
