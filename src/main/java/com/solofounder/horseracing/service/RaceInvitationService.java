package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.invitation.CreateInvitationRequest;
import com.solofounder.horseracing.dto.invitation.InvitationResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.RaceInvitationRepository;
import com.solofounder.horseracing.repository.RaceRegistrationRepository;
import com.solofounder.horseracing.repository.StaffRepository;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceInvitationService {

    private static final Set<RaceInvitationStatus> OPEN_INVITATION_STATUSES =
            Set.of(RaceInvitationStatus.SENT, RaceInvitationStatus.PENDING);

    private final RaceInvitationRepository raceInvitationRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final JockeyRepository jockeyRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final RaceService raceService;

    public InvitationResponse createInvitation(CreateInvitationRequest request) {
        User owner = getCurrentUser();
        requireRole(owner, Role.OWNER);

        RaceRegistration registration = raceRegistrationRepository.findById(request.getRaceRegistrationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race registration not found"));
        Jockey jockey = jockeyRepository.findById(request.getJockeyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey not found"));

        if (!registration.getSubmittedBy().getUserId().equals(owner.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        if (registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race registration must be approved before inviting jockey");
        }
        if (!raceService.isOpenForRegistration(registration.getRace(), LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is not open for registration");
        }
        if (raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndStatusIn(
                registration.getRegistrationId(), jockey.getJockeyId(), OPEN_INVITATION_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already exists for this registration and jockey");
        }
        if (raceInvitationRepository.existsByRaceRegistrationRegistrationIdAndStatus(
                registration.getRegistrationId(), RaceInvitationStatus.ACCEPTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Race registration already has an accepted invitation");
        }

        LocalDateTime now = LocalDateTime.now();
        RaceInvitation invitation = RaceInvitation.builder()
                .raceRegistration(registration)
                .jockey(jockey)
                .status(RaceInvitationStatus.SENT)
                .message(normalizeMessage(request.getMessage()))
                .sentAt(now)
                .createdAt(now)
                .build();
        return toResponse(raceInvitationRepository.save(invitation));
    }

    public List<InvitationResponse> getInvitations() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.OWNER) {
            return raceInvitationRepository.findByRaceRegistrationSubmittedByUserId(currentUser.getUserId()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (currentUser.getRole() == Role.JOCKEY) {
            Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));
            return raceInvitationRepository.findByJockeyJockeyId(jockey.getJockeyId()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return raceInvitationRepository.findAll().stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (currentUser.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
            return raceInvitationRepository.findAll().stream()
                    .filter(invitation -> {
                        Staff raceStaff = invitation.getRaceRegistration().getRace().getStaff();
                        return raceStaff != null && raceStaff.getStaffId().equals(staff.getStaffId());
                    })
                    .map(this::toResponse)
                    .toList();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    public InvitationResponse acceptInvitation(Long invitationId) {
        return respondToInvitation(invitationId, RaceInvitationStatus.ACCEPTED);
    }

    public InvitationResponse declineInvitation(Long invitationId) {
        return respondToInvitation(invitationId, RaceInvitationStatus.DECLINED);
    }

    private InvitationResponse respondToInvitation(Long invitationId, RaceInvitationStatus newStatus) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.JOCKEY);
        Jockey jockey = jockeyRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey profile not found"));
        RaceInvitation invitation = raceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!invitation.getJockey().getJockeyId().equals(jockey.getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        if (!OPEN_INVITATION_STATUSES.contains(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation has already been responded to");
        }

        invitation.setStatus(newStatus);
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

    private String normalizeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        String normalized = message.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Message must be at most 500 characters");
        }
        return normalized;
    }

    private InvitationResponse toResponse(RaceInvitation invitation) {
        RaceRegistration registration = invitation.getRaceRegistration();
        Race race = registration.getRace();
        Horse horse = registration.getHorse();
        User owner = registration.getSubmittedBy();
        Jockey jockey = invitation.getJockey();
        User jockeyUser = jockey.getUser();
        boolean canRespond = OPEN_INVITATION_STATUSES.contains(invitation.getStatus());
        return InvitationResponse.builder()
                .invitationId(invitation.getInvitationId())
                .raceRegistrationId(registration.getRegistrationId())
                .raceId(race.getRaceId())
                .raceName(race.getRaceName())
                .horseId(horse.getHorseId())
                .horseName(horse.getHorseName())
                .ownerId(owner.getUserId())
                .ownerName(owner.getFullName())
                .jockeyId(jockey.getJockeyId())
                .jockeyName(jockeyUser.getFullName())
                .status(invitation.getStatus() != null ? invitation.getStatus().name() : null)
                .message(invitation.getMessage())
                .sentAt(invitation.getSentAt())
                .respondedAt(invitation.getRespondedAt())
                .createdAt(invitation.getCreatedAt())
                .canAccept(canRespond)
                .canDecline(canRespond)
                .build();
    }
}
