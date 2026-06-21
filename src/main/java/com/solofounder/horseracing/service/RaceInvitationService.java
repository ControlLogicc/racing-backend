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

    public InvitationResponse createInvitation(CreateInvitationRequest request) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.OWNER);

        RaceRegistration registration = raceRegistrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        // Registration status must be APPROVED
        if (registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not APPROVED");
        }

        // Authenticated Owner must own the Horse in the Registration
        Horse horse = registration.getHorse();
        if (horse == null || horse.getOwner() == null || !horse.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"); // 404 convention
        }

        // Validate Race status
        Race race = registration.getRace();
        if (race == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found");
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
        boolean hasAcceptedOrUsed = raceInvitationRepository.existsByRegistrationRegistrationIdAndInvitationStatus(
                registration.getRegistrationId(), RaceInvitationStatus.ACCEPTED) ||
                raceInvitationRepository.existsByRegistrationRegistrationIdAndInvitationStatus(
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
        boolean alreadyInvited = raceInvitationRepository.existsByRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatusIn(
                registration.getRegistrationId(),
                jockey.getJockeyId(),
                Arrays.asList(RaceInvitationStatus.SENT, RaceInvitationStatus.ACCEPTED, RaceInvitationStatus.USED)
        );
        if (alreadyInvited) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jockey has already been invited for this registration");
        }

        RaceInvitation invitation = RaceInvitation.builder()
                .registration(registration)
                .jockey(jockey)
                .invitationStatus(RaceInvitationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .message(request.getMessage())
                .build();

        return toResponse(raceInvitationRepository.save(invitation));
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyInvitations(String statusStr) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);

        Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));

        List<RaceInvitation> invitations;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                RaceInvitationStatus status = RaceInvitationStatus.valueOf(statusStr.toUpperCase());
                invitations = raceInvitationRepository.findByJockeyUserUserIdAndInvitationStatus(currentUser.getUserId(), status);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status filter");
            }
        } else {
            invitations = raceInvitationRepository.findByJockeyUserUserId(currentUser.getUserId());
        }

        return invitations.stream()
                .map(this::toResponse)
                .toList();
    }

    public InvitationResponse acceptInvitation(Long invitationId) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);

        Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));

        RaceInvitation invitation = raceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        // Ownership rule: invitation.jockey must match the authenticated Jockey
        if (!invitation.getJockey().getJockeyId().equals(jockey.getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"); // 404 convention
        }

        if (invitation.getInvitationStatus() != RaceInvitationStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is not in SENT status");
        }

        RaceRegistration registration = invitation.getRegistration();
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
        boolean hasAcceptedOrUsed = raceInvitationRepository.existsByRegistrationRegistrationIdAndInvitationStatus(
                registration.getRegistrationId(), RaceInvitationStatus.ACCEPTED) ||
                raceInvitationRepository.existsByRegistrationRegistrationIdAndInvitationStatus(
                registration.getRegistrationId(), RaceInvitationStatus.USED);
        if (hasAcceptedOrUsed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another invitation for this registration is already accepted");
        }

        invitation.setInvitationStatus(RaceInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());

        return toResponse(raceInvitationRepository.save(invitation));
    }

    public InvitationResponse declineInvitation(Long invitationId) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);

        Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));

        RaceInvitation invitation = raceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        // Ownership rule: invitation.jockey must match the authenticated Jockey
        if (!invitation.getJockey().getJockeyId().equals(jockey.getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"); // 404 convention
        }

        if (invitation.getInvitationStatus() != RaceInvitationStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is not in SENT status");
        }

        invitation.setInvitationStatus(RaceInvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());

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
        return InvitationResponse.builder()
                .invitationId(invitation.getInvitationId())
                .registrationId(invitation.getRegistration().getRegistrationId())
                .raceId(invitation.getRegistration().getRace().getRaceId())
                .raceName(invitation.getRegistration().getRace().getRaceName())
                .horseId(invitation.getRegistration().getHorse().getHorseId())
                .horseName(invitation.getRegistration().getHorse().getHorseName())
                .ownerId(invitation.getRegistration().getHorse().getOwner().getUserId())
                .ownerName(invitation.getRegistration().getHorse().getOwner().getFullName())
                .jockeyId(invitation.getJockey().getJockeyId())
                .jockeyName(invitation.getJockey().getUser().getFullName())
                .status(invitation.getInvitationStatus().name())
                .sentAt(invitation.getSentAt())
                .respondedAt(invitation.getRespondedAt())
                .message(invitation.getMessage())
                .build();
    }
}
