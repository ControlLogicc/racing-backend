package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.invitation.CreateInvitationRequest;
import com.solofounder.horseracing.dto.invitation.InvitationResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceInvitationService {

    private final RaceInvitationRepository raceInvitationRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final JockeyRepository jockeyRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RaceEntryRepository raceEntryRepository;
    private final StaffRepository staffRepository;

    public InvitationResponse createInvitation(CreateInvitationRequest request) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.OWNER);

        RaceRegistration registration = raceRegistrationRepository.findById(request.getRaceRegistrationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        // Registration status must be APPROVED
        if (registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not APPROVED");
        }

        // Authenticated Owner must own the Horse in the Registration
        Horse horse = registration.getHorse();
        if (horse == null || horse.getOwner() == null || !horse.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        // Validate Race status
        Race race = registration.getRace();
        if (race == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (race.getRegistrationCloseAt() != null && now.isAfter(race.getRegistrationCloseAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race registration period is closed");
        }

        if (race.getStatus() == RaceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is cancelled");
        }
        if (race.getStatus() == RaceStatus.RUNNING || race.getStatus() == RaceStatus.RESULT_PENDING || race.getStatus() == RaceStatus.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race no longer accepts Jockey assignment");
        }

        // Check if registration already has an Entry (via native database query)
        Integer entryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.race_entry WHERE registration_id = ?",
                Integer.class,
                registration.getRegistrationId()
        );
        if (entryCount != null && entryCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration already has a race entry");
        }

        // Check if Registration already has an ACCEPTED or USED invitation
        boolean hasAcceptedOrUsed = raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndInvitationStatus(
                registration.getRegistrationId(), RaceInvitationStatus.ACCEPTED) ||
                raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndInvitationStatus(
                registration.getRegistrationId(), RaceInvitationStatus.USED);
        if (hasAcceptedOrUsed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already has an ACCEPTED invitation");
        }

        // Load and validate Jockey
        Jockey jockey = jockeyRepository.findById(request.getJockeyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey not found"));

        if (jockey.getUser() == null || jockey.getUser().getRole() != Role.JOCKEY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Jockey profile");
        }

        if (jockey.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jockey account is not active");
        }

        // Check if this Jockey has already been invited for this Registration in active status
        boolean alreadyInvited = raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatusIn(
                registration.getRegistrationId(),
                jockey.getJockeyId(),
                Arrays.asList(RaceInvitationStatus.SENT, RaceInvitationStatus.ACCEPTED, RaceInvitationStatus.USED)
        );
        if (alreadyInvited) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jockey has already been invited for this registration");
        }

        RaceInvitation invitation = RaceInvitation.builder()
                .raceRegistration(registration)
                .jockey(jockey)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .message(request.getMessage())
                .build();

        return toResponse(raceInvitationRepository.save(invitation));
    }

    @Transactional
    public List<InvitationResponse> getInvitations(String statusStr) {
        User currentUser = getCurrentUser();

        RaceInvitationStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = RaceInvitationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status filter");
            }
        }

        List<RaceInvitation> invitations;
        if (currentUser.getRole() == Role.JOCKEY) {
            jockeyRepository.findByUserUserId(currentUser.getUserId())
                    .orElseGet(() -> jockeyRepository.save(
                            Jockey.builder()
                                    .user(currentUser)
                                    .status("available")
                                    .build()
                    ));
            invitations = raceInvitationRepository.findByJockeyUserUserId(currentUser.getUserId());
        } else if (currentUser.getRole() == Role.OWNER) {
            invitations = raceInvitationRepository.findByRaceRegistrationHorseOwnerUserId(currentUser.getUserId());
        } else if (currentUser.getRole() == Role.ADMIN) {
            invitations = raceInvitationRepository.findAll();
        } else if (currentUser.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
            invitations = raceInvitationRepository.findAll().stream()
                    .filter(inv -> inv.getRaceRegistration().getRace().getStaff() != null &&
                            inv.getRaceRegistration().getRace().getStaff().getStaffId().equals(staff.getStaffId()))
                    .toList();
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        LocalDateTime now = LocalDateTime.now();
        for (RaceInvitation inv : invitations) {
            if ((inv.getInvitationStatus() == RaceInvitationStatus.SENT || inv.getInvitationStatus() == RaceInvitationStatus.PENDING_RESPONSE)
                    && inv.getRaceRegistration().getRace().getRegistrationCloseAt() != null
                    && now.isAfter(inv.getRaceRegistration().getRace().getRegistrationCloseAt())) {
                inv.setInvitationStatus(RaceInvitationStatus.EXPIRED);
                inv.setRespondedAt(now);
                raceInvitationRepository.save(inv);
            }
        }

        if (status != null) {
            final RaceInvitationStatus finalStatus = status;
            invitations = invitations.stream()
                    .filter(inv -> inv.getInvitationStatus() == finalStatus)
                    .toList();
        }

        return invitations.stream()
                .map(this::toResponse)
                .toList();
    }

    public InvitationResponse acceptInvitation(Long invitationId) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);

        Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseGet(() -> jockeyRepository.save(
                        Jockey.builder()
                                .user(currentUser)
                                .status("available")
                                .build()
                ));

        RaceInvitation invitation = raceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        // Ownership rule: invitation's jockey must belong to the authenticated user
        if (!invitation.getJockey().getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        if (invitation.getInvitationStatus() != RaceInvitationStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation is already responded");
        }

        RaceRegistration registration = invitation.getRaceRegistration();
        if (registration == null || registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not APPROVED");
        }

        Race race = registration.getRace();
        if (race == null || race.getStatus() == RaceStatus.CANCELLED ||
            race.getStatus() == RaceStatus.RUNNING ||
            race.getStatus() == RaceStatus.RESULT_PENDING ||
            race.getStatus() == RaceStatus.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is not in valid status");
        }

        LocalDateTime now = LocalDateTime.now();
        if (race.getRegistrationCloseAt() != null && now.isAfter(race.getRegistrationCloseAt())) {
            invitation.setInvitationStatus(RaceInvitationStatus.EXPIRED);
            invitation.setRespondedAt(now);
            raceInvitationRepository.save(invitation);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired because race registration period is closed");
        }

        // Check if registration already has an Entry (via native database query)
        Integer entryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.race_entry WHERE registration_id = ?",
                Integer.class,
                registration.getRegistrationId()
        );
        if (entryCount != null && entryCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration already has a race entry");
        }

        // Check if another invitation for the same registration is already ACCEPTED or USED
        // (must run BEFORE we save ACCEPTED status to avoid self-detection)
        boolean hasAcceptedOrUsed = raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndInvitationStatus(
                registration.getRegistrationId(), RaceInvitationStatus.ACCEPTED) ||
                raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndInvitationStatus(
                registration.getRegistrationId(), RaceInvitationStatus.USED);
        if (hasAcceptedOrUsed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another invitation for this registration is already accepted");
        }

        // Check if unique race + horse/jockey BEFORE saving (to avoid partial state on rollback)
        if (raceEntryRepository.existsByRaceRaceIdAndHorseHorseId(race.getRaceId(), registration.getHorse().getHorseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Horse is already entered in this race");
        }
        if (raceEntryRepository.existsByRaceRaceIdAndJockeyJockeyId(race.getRaceId(), invitation.getJockey().getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jockey is already entered in this race");
        }

        // All checks passed — persist ACCEPTED status now
        invitation.setInvitationStatus(RaceInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(now);
        invitation = raceInvitationRepository.save(invitation);

        final RaceRegistration finalRegistration = registration;
        final RaceInvitation finalInvitation = invitation;

        // Auto-create RaceEntry
        RaceEntry entry = raceEntryRepository.findByRegistrationRegistrationId(registration.getRegistrationId())
                .orElseGet(() -> {
                    Horse horse = finalRegistration.getHorse();
                    BigDecimal currentScore = horse.getCurrentScore() != null ? horse.getCurrentScore() : BigDecimal.ZERO;
                    short horseClass = horse.getHorseClass() != null ? horse.getHorseClass() : 5;

                    BigDecimal baseWeight = new BigDecimal("50.0");
                    BigDecimal minScore = BigDecimal.ZERO;
                    switch (horseClass) {
                        case 1: minScore = new BigDecimal("80.0"); break;
                        case 2: minScore = new BigDecimal("60.0"); break;
                        case 3: minScore = new BigDecimal("40.0"); break;
                        case 4: minScore = new BigDecimal("20.0"); break;
                        default: minScore = BigDecimal.ZERO; break;
                    }

                    BigDecimal scoreInClass = currentScore.subtract(minScore);
                    if (scoreInClass.compareTo(BigDecimal.ZERO) < 0) {
                        scoreInClass = BigDecimal.ZERO;
                    }

                    BigDecimal calculatedHandicapWeight = baseWeight.add(scoreInClass.multiply(new BigDecimal("0.5")));

                    RaceEntry newEntry = RaceEntry.builder()
                            .race(race)
                            .registration(finalRegistration)
                            .invitation(finalInvitation)
                            .horse(horse)
                            .jockey(finalInvitation.getJockey())
                            .gateNumber(null)
                            .handicapWeight(calculatedHandicapWeight)
                            .entryStatus("DECLARED")
                            .build();
                    return raceEntryRepository.save(newEntry);
                });

        return toResponse(invitation);
    }

    public InvitationResponse declineInvitation(Long invitationId) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);

        Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseGet(() -> jockeyRepository.save(
                        Jockey.builder()
                                .user(currentUser)
                                .status("available")
                                .build()
                ));

        RaceInvitation invitation = raceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        // Ownership rule: invitation.jockey must match the authenticated Jockey
        if (!invitation.getJockey().getJockeyId().equals(jockey.getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        if (invitation.getInvitationStatus() != RaceInvitationStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation is already responded");
        }

        RaceRegistration registration = invitation.getRaceRegistration();
        Race race = registration.getRace();
        LocalDateTime now = LocalDateTime.now();
        if (race.getRegistrationCloseAt() != null && now.isAfter(race.getRegistrationCloseAt())) {
            invitation.setInvitationStatus(RaceInvitationStatus.EXPIRED);
            invitation.setRespondedAt(now);
            raceInvitationRepository.save(invitation);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired because race registration period is closed");
        }

        invitation.setInvitationStatus(RaceInvitationStatus.DECLINED);
        invitation.setRespondedAt(now);

        return toResponse(raceInvitationRepository.save(invitation));
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

    private InvitationResponse toResponse(RaceInvitation invitation) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closeAt = invitation.getRaceRegistration().getRace().getRegistrationCloseAt();
        boolean hasExpired = invitation.getInvitationStatus() == RaceInvitationStatus.EXPIRED || (closeAt != null && now.isAfter(closeAt));
        boolean canAct = invitation.getInvitationStatus() == RaceInvitationStatus.SENT && !hasExpired;

        Long entryId = raceEntryRepository.findByRegistrationRegistrationId(invitation.getRaceRegistration().getRegistrationId())
                .map(RaceEntry::getEntryId)
                .orElse(null);

        return InvitationResponse.builder()
                .invitationId(invitation.getInvitationId())
                .raceRegistrationId(invitation.getRaceRegistration().getRegistrationId())
                .raceId(invitation.getRaceRegistration().getRace().getRaceId())
                .raceName(invitation.getRaceRegistration().getRace().getRaceName())
                .horseId(invitation.getRaceRegistration().getHorse().getHorseId())
                .horseName(invitation.getRaceRegistration().getHorse().getHorseName())
                .ownerId(invitation.getRaceRegistration().getHorse().getOwner().getUserId())
                .ownerName(invitation.getRaceRegistration().getHorse().getOwner().getFullName())
                .jockeyId(invitation.getJockey().getJockeyId())
                .jockeyName(invitation.getJockey().getUser().getFullName())
                .status(hasExpired ? RaceInvitationStatus.EXPIRED.name() : invitation.getInvitationStatus().name())
                .sentAt(invitation.getSentAt())
                .respondedAt(invitation.getRespondedAt())
                .message(invitation.getMessage())
                .canAccept(canAct)
                .canDecline(canAct)
                .entryId(entryId)
                .scheduledTime(invitation.getRaceRegistration().getRace().getScheduledTime())
                .build();
    }
}
