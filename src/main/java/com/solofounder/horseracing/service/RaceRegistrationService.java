package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.registration.CreateRegistrationRequest;
import com.solofounder.horseracing.dto.registration.ApprovedRegistrationForInvitationResponse;
import com.solofounder.horseracing.dto.registration.RegistrationResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.HorseRegistrationType;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceRegistrationService {

    private static final List<RaceInvitationStatus> ACTIVE_INVITATION_STATUSES = List.of(
            RaceInvitationStatus.SENT,
            RaceInvitationStatus.PENDING_RESPONSE,
            RaceInvitationStatus.ACCEPTED,
            RaceInvitationStatus.USED
    );

    private final RaceRegistrationRepository raceRegistrationRepository;
    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final RaceInvitationRepository raceInvitationRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceService raceService;

    public RegistrationResponse createRegistration(CreateRegistrationRequest request) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.OWNER);

        Race race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));

        Horse horse = horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));

        // Horse must belong to the authenticated Owner
        if (horse.getOwner() == null || !horse.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"); // 404 per convention
        }

        // Horse status must be active
        if (horse.getStatus() == null || !"active".equalsIgnoreCase(horse.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse status is not active");
        }

        // PREVIOUSLY_REGISTERED horses must have their rating verified by Staff before race registration
        if (horse.getRegistrationType() == HorseRegistrationType.PREVIOUSLY_REGISTERED
                && !horse.isRatingVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Horse rating has not been verified by Staff. Please wait for rating verification before registering.");
        }

        // Race must be open for registration
        if (!raceService.isOpenForRegistration(race, LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is not open for registration");
        }

        // Duplicate registration check
        if (raceRegistrationRepository.existsByRaceRaceIdAndHorseHorseId(request.getRaceId(), request.getHorseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Horse is already registered for this race");
        }

        // Satisfy class requirement check if present
        RaceCondition condition = race.getRaceCondition();
        if (condition != null && condition.getClassRequirement() != null && !condition.getClassRequirement().isBlank()) {
            if (!isClassSatisfied(horse.getHorseClass(), condition.getClassRequirement())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse class does not satisfy race condition requirement");
            }
        }

        RaceRegistration registration = RaceRegistration.builder()
                .race(race)
                .horse(horse)
                .submittedBy(currentUser)
                .status(RaceRegistrationStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(raceRegistrationRepository.save(registration));
    }

    public List<RegistrationResponse> getRegistrationsForRace(Long raceId) {
        User currentUser = getCurrentUser();
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));

        if (currentUser.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
            if (race.getStaff() == null || !race.getStaff().getStaffId().equals(staff.getStaffId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        return raceRegistrationRepository.findByRaceRaceId(raceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovedRegistrationForInvitationResponse> getApprovedRegistrationsForCurrentOwner() {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.OWNER);

        return raceRegistrationRepository
                .findBySubmittedByUserIdAndStatusWithDetails(currentUser.getUserId(), RaceRegistrationStatus.APPROVED)
                .stream()
                .map(this::toApprovedRegistrationForInvitationResponse)
                .toList();
    }

    public RegistrationResponse approveRegistration(Long registrationId) {
        User currentUser = getCurrentUser();
        RaceRegistration registration = raceRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (registration.getStatus() != RaceRegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not in PENDING status");
        }

        Race race = registration.getRace();
        Staff staff = null;
        if (currentUser.getRole() == Role.STAFF) {
            staff = staffRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
            if (race.getStaff() == null || !race.getStaff().getStaffId().equals(staff.getStaffId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        // Validate Race status
        RaceStatus raceStatus = race.getStatus();
        if (raceStatus == RaceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is cancelled");
        }
        if (raceStatus == RaceStatus.RUNNING || raceStatus == RaceStatus.RESULT_PENDING || raceStatus == RaceStatus.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race has already started or completed");
        }

        // Verify Horse is active
        Horse horse = registration.getHorse();
        if (horse.getStatus() == null || !"active".equalsIgnoreCase(horse.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse status is not active");
        }

        // Verify capacity
        RaceCondition condition = race.getRaceCondition();
        if (condition != null && condition.getMaxEntries() != null) {
            long approvedCount = raceRegistrationRepository.countByRaceRaceIdAndStatus(race.getRaceId(), RaceRegistrationStatus.APPROVED);
            if (approvedCount >= condition.getMaxEntries()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race registration capacity reached");
            }
        }

        registration.setStatus(RaceRegistrationStatus.APPROVED);
        registration.setReviewedAt(LocalDateTime.now());
        if (staff != null) {
            registration.setApprovedBy(staff);
        } else {
            // ADMIN approving - set approvedBy to the assigned staff if available
            registration.setApprovedBy(race.getStaff());
        }

        return toResponse(raceRegistrationRepository.save(registration));
    }

    public RegistrationResponse rejectRegistration(Long registrationId) {
        User currentUser = getCurrentUser();
        RaceRegistration registration = raceRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (registration.getStatus() != RaceRegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not in PENDING status");
        }

        Race race = registration.getRace();
        Staff staff = null;
        if (currentUser.getRole() == Role.STAFF) {
            staff = staffRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
            if (race.getStaff() == null || !race.getStaff().getStaffId().equals(staff.getStaffId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        registration.setStatus(RaceRegistrationStatus.REJECTED);
        registration.setReviewedAt(LocalDateTime.now());
        if (staff != null) {
            registration.setApprovedBy(staff);
        } else {
            registration.setApprovedBy(race.getStaff());
        }

        return toResponse(raceRegistrationRepository.save(registration));
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

    private boolean isClassSatisfied(Short horseClass, String classReq) {
        if (classReq == null || classReq.isBlank()) {
            return true;
        }
        String req = classReq.trim().toUpperCase().replace("CLASS", "").trim();
        if (req.contains("-")) {
            String[] parts = req.split("-");
            try {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                int low = Math.min(min, max);
                int high = Math.max(min, max);
                return horseClass >= low && horseClass <= high;
            } catch (NumberFormatException e) {
                return true;
            }
        } else {
            try {
                int reqClass = Integer.parseInt(req);
                return horseClass == reqClass;
            } catch (NumberFormatException e) {
                return true;
            }
        }
    }

    private ApprovedRegistrationForInvitationResponse toApprovedRegistrationForInvitationResponse(RaceRegistration registration) {
        Race race = registration.getRace();
        Horse horse = registration.getHorse();
        RaceInvitation latestInvitation = raceInvitationRepository
                .findTopByRaceRegistrationRegistrationIdOrderByCreatedAtDesc(registration.getRegistrationId())
                .orElse(null);
        RaceEntry entry = raceEntryRepository
                .findByRegistrationRegistrationId(registration.getRegistrationId())
                .orElse(null);
        boolean hasActiveInvitation = raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndInvitationStatusIn(
                registration.getRegistrationId(),
                ACTIVE_INVITATION_STATUSES);
        boolean registrationStillOpen = race.getRegistrationCloseAt() == null
                || !LocalDateTime.now().isAfter(race.getRegistrationCloseAt());

        boolean canInviteJockey = registration.getStatus() == RaceRegistrationStatus.APPROVED
                && registrationStillOpen
                && entry == null
                && !hasActiveInvitation;

        return ApprovedRegistrationForInvitationResponse.builder()
                .registrationId(registration.getRegistrationId())
                .raceId(race.getRaceId())
                .raceName(race.getRaceName())
                .horseId(horse.getHorseId())
                .horseName(horse.getHorseName())
                .registrationStatus(registration.getStatus() != null ? registration.getStatus().name() : null)
                .submittedAt(registration.getSubmittedAt())
                .reviewedAt(registration.getReviewedAt())
                .registrationCloseAt(race.getRegistrationCloseAt())
                .scheduledTime(race.getScheduledTime())
                .invitationId(latestInvitation != null ? latestInvitation.getInvitationId() : null)
                .invitationStatus(latestInvitation != null && latestInvitation.getInvitationStatus() != null
                        ? latestInvitation.getInvitationStatus().name()
                        : null)
                .entryId(entry != null ? entry.getEntryId() : null)
                .canInviteJockey(canInviteJockey)
                .build();
    }

    private RegistrationResponse toResponse(RaceRegistration registration) {
        return RegistrationResponse.builder()
                .registrationId(registration.getRegistrationId())
                .raceId(registration.getRace().getRaceId())
                .raceName(registration.getRace().getRaceName())
                .horseId(registration.getHorse().getHorseId())
                .horseName(registration.getHorse().getHorseName())
                .ownerId(registration.getSubmittedBy().getUserId())
                .ownerName(registration.getSubmittedBy().getFullName())
                .status(registration.getStatus() != null ? registration.getStatus().name() : null)
                .build();
    }
}
