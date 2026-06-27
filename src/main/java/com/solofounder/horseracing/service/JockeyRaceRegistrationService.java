package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.jockey.CreateJockeyRaceRegistrationRequest;
import com.solofounder.horseracing.dto.jockey.EligibleJockeyForInvitationResponse;
import com.solofounder.horseracing.dto.jockey.JockeyRaceRegistrationResponse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.JockeyRaceRegistration;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.JockeyRaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.JockeyRaceRegistrationRepository;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceInvitationRepository;
import com.solofounder.horseracing.repository.RaceRegistrationRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.UserRepository;
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
public class JockeyRaceRegistrationService {

    private static final List<RaceInvitationStatus> ACTIVE_INVITATION_STATUSES = List.of(
            RaceInvitationStatus.SENT,
            RaceInvitationStatus.PENDING_RESPONSE,
            RaceInvitationStatus.ACCEPTED,
            RaceInvitationStatus.USED
    );

    private final JockeyRaceRegistrationRepository jockeyRaceRegistrationRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final RaceInvitationRepository raceInvitationRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceRepository raceRepository;
    private final JockeyRepository jockeyRepository;
    private final UserRepository userRepository;
    private final RaceService raceService;

    public JockeyRaceRegistrationResponse registerCurrentJockey(
            CreateJockeyRaceRegistrationRequest request) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);

        Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));
        requireAvailableJockey(jockey);

        Race race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        if (!raceService.isOpenForRegistration(race, LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is not open for registration");
        }
        if (jockeyRaceRegistrationRepository.existsByRaceRaceIdAndJockeyJockeyIdAndStatus(
                race.getRaceId(),
                jockey.getJockeyId(),
                JockeyRaceRegistrationStatus.REGISTERED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jockey is already registered for this race");
        }

        JockeyRaceRegistration registration = JockeyRaceRegistration.builder()
                .race(race)
                .jockey(jockey)
                .status(JockeyRaceRegistrationStatus.REGISTERED)
                .note(trimToNull(request.getNote()))
                .registeredAt(LocalDateTime.now())
                .build();
        return toResponse(jockeyRaceRegistrationRepository.save(registration));
    }

    @Transactional(readOnly = true)
    public List<JockeyRaceRegistrationResponse> getCurrentJockeyRegistrations() {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);
        jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));

        return jockeyRaceRegistrationRepository
                .findByJockeyUserUserIdOrderByRegisteredAtDesc(currentUser.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EligibleJockeyForInvitationResponse> getEligibleJockeys(Long registrationId) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.OWNER);

        RaceRegistration ownerRegistration = raceRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        if (ownerRegistration.getSubmittedBy() == null
                || !ownerRegistration.getSubmittedBy().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        if (ownerRegistration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not APPROVED");
        }

        Race race = ownerRegistration.getRace();
        if (race.getRegistrationCloseAt() != null
                && LocalDateTime.now().isAfter(race.getRegistrationCloseAt())) {
            return List.of();
        }
        if (raceEntryRepository.existsByRegistrationRegistrationId(registrationId)) {
            return List.of();
        }

        return jockeyRaceRegistrationRepository
                .findByRaceIdAndStatusWithJockey(race.getRaceId(), JockeyRaceRegistrationStatus.REGISTERED)
                .stream()
                .filter(jockeyRegistration -> isEligible(registrationId, race.getRaceId(), jockeyRegistration.getJockey()))
                .map(jockeyRegistration -> toEligibleResponse(ownerRegistration, jockeyRegistration))
                .toList();
    }

    private boolean isEligible(Long registrationId, Long raceId, Jockey jockey) {
        return isAvailable(jockey)
                && !raceEntryRepository.existsByRaceRaceIdAndJockeyJockeyId(raceId, jockey.getJockeyId())
                && !raceInvitationRepository
                .existsByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatusIn(
                        registrationId,
                        jockey.getJockeyId(),
                        ACTIVE_INVITATION_STATUSES);
    }

    private EligibleJockeyForInvitationResponse toEligibleResponse(
            RaceRegistration ownerRegistration,
            JockeyRaceRegistration jockeyRegistration) {
        Jockey jockey = jockeyRegistration.getJockey();
        return EligibleJockeyForInvitationResponse.builder()
                .jockeyRaceRegistrationId(jockeyRegistration.getJockeyRaceRegistrationId())
                .registrationId(ownerRegistration.getRegistrationId())
                .raceId(ownerRegistration.getRace().getRaceId())
                .jockeyId(jockey.getJockeyId())
                .jockeyName(jockey.getUser().getFullName())
                .weight(jockey.getWeight())
                .experienceYears(jockey.getExperienceYears() == null
                        ? null
                        : jockey.getExperienceYears().intValue())
                .jockeyStatus(jockey.getStatus())
                .raceRegistrationStatus(jockeyRegistration.getStatus().name())
                .registeredAt(jockeyRegistration.getRegisteredAt())
                .canInvite(true)
                .reason(null)
                .build();
    }

    private JockeyRaceRegistrationResponse toResponse(JockeyRaceRegistration registration) {
        Jockey jockey = registration.getJockey();
        Race race = registration.getRace();
        return JockeyRaceRegistrationResponse.builder()
                .jockeyRaceRegistrationId(registration.getJockeyRaceRegistrationId())
                .raceId(race.getRaceId())
                .raceName(race.getRaceName())
                .jockeyId(jockey.getJockeyId())
                .jockeyName(jockey.getUser().getFullName())
                .jockeyStatus(jockey.getStatus())
                .registrationStatus(registration.getStatus().name())
                .note(registration.getNote())
                .registeredAt(registration.getRegisteredAt())
                .updatedAt(registration.getUpdatedAt())
                .build();
    }

    private void requireAvailableJockey(Jockey jockey) {
        if (!isAvailable(jockey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jockey status is not available");
        }
    }

    private boolean isAvailable(Jockey jockey) {
        return jockey.getStatus() != null && "available".equalsIgnoreCase(jockey.getStatus());
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

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
