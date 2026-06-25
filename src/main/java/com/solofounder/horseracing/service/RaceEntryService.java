package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.entry.*;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.*;
import com.solofounder.horseracing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceEntryService {

    private final RaceEntryRepository raceEntryRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final RaceInvitationRepository raceInvitationRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final RefereeRepository refereeRepository;
    private final RaceRepository raceRepository;
    private final JockeyRaceRegistrationRepository jockeyRaceRegistrationRepository;

    public RaceEntryResponse createEntry(CreateRaceEntryRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.STAFF && currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        RaceInvitation invitation = raceInvitationRepository.findByIdWithEntryDetails(request.getInvitationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getInvitationStatus() != RaceInvitationStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is not ACCEPTED");
        }

        RaceRegistration registration = invitation.getRaceRegistration();
        if (registration == null || registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not APPROVED");
        }

        Race race = registration.getRace();
        requireEntryCreationPermission(currentUser, race);
        validateRaceAllowsEntryCreation(race);

        if (!jockeyRaceRegistrationRepository.existsByRaceRaceIdAndJockeyJockeyIdAndStatus(
                race.getRaceId(),
                invitation.getJockey().getJockeyId(),
                JockeyRaceRegistrationStatus.REGISTERED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jockey has not registered for this race");
        }

        if (raceEntryRepository.existsByRegistrationRegistrationId(registration.getRegistrationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already has an entry");
        }
        if (raceEntryRepository.existsByInvitationInvitationId(invitation.getInvitationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already has an entry");
        }
        if (raceEntryRepository.existsByRaceRaceIdAndJockeyJockeyId(race.getRaceId(),
                invitation.getJockey().getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jockey is already entered in this race");
        }

        if (raceEntryRepository.existsByRaceRaceIdAndHorseHorseId(race.getRaceId(),
                registration.getHorse().getHorseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Horse is already entered in this race");
        }

        RaceEntry entry = RaceEntry.builder()
                .race(race)
                .registration(registration)
                .invitation(invitation)
                .horse(registration.getHorse())
                .jockey(invitation.getJockey())
                .gateNumber(resolveInitialGateNumber(race.getRaceId()))
                .entryStatus("declared")
                .build();

        RaceEntry savedEntry;
        try {
            savedEntry = raceEntryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            rethrowConflictIfRaceEntryDuplicate(registration, invitation, race);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Race entry violates database constraint",
                    ex);
        }

        invitation.setInvitationStatus(RaceInvitationStatus.USED);
        raceInvitationRepository.saveAndFlush(invitation);

        return toResponse(savedEntry);
    }

    @Transactional(readOnly = true)
    public RaceEntryResponse getEntry(Long id) {
        RaceEntry entry = raceEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<RaceEntryResponse> getEntriesForRace(Long raceId) {
        return raceEntryRepository.findByRaceRaceIdWithDetails(raceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcceptedInvitationCandidateResponse> getAcceptedInvitationCandidates(Long raceId) {
        User currentUser = getCurrentUser();
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        requireEntryCreationPermission(currentUser, race);

        return raceInvitationRepository
                .findByRaceIdAndInvitationStatusWithDetails(raceId, RaceInvitationStatus.ACCEPTED)
                .stream()
                .map(this::toCandidateResponse)
                .toList();
    }

    public List<RaceEntryResponse> batchWeightCheck(Long raceId, BatchWeightCheckRequest request) {
        User currentUser = getCurrentUser();
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        requireWeightCheckPermission(currentUser, race);

        if (race.getStatus() == RaceStatus.OFFICIAL || race.getStatus() == RaceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race status does not allow weight check");
        }

        List<RaceEntry> updatedEntries = request.getChecks().stream()
                .map(check -> applyWeightCheck(raceId, request, check))
                .toList();

        return raceEntryRepository.saveAll(updatedEntries).stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceEntryResponse updateWeight(Long id, UpdateWeightRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.STAFF && currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        RaceEntry entry = raceEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));

        BigDecimal handicapWeight = entry.getHandicapWeight();
        BigDecimal jockeyActualWeight = request.getActualWeight();
        BigDecimal leadWeight = BigDecimal.ZERO;
        BigDecimal carriedWeight = jockeyActualWeight;

        if (handicapWeight != null) {
            leadWeight = handicapWeight.subtract(jockeyActualWeight).max(BigDecimal.ZERO);
            carriedWeight = jockeyActualWeight.add(leadWeight);
        }

        entry.setJockeyActualWeight(jockeyActualWeight);
        entry.setLeadWeight(leadWeight);
        entry.setCarriedWeight(carriedWeight);
        entry.setWeightCheckStatus(request.getWeightCheckStatus());

        return toResponse(raceEntryRepository.save(entry));
    }

    private RaceEntry applyWeightCheck(Long raceId, BatchWeightCheckRequest request, WeightCheckItemRequest check) {
        RaceEntry entry = raceEntryRepository.findByIdWithDetails(check.getEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));

        if (!entry.getRace().getRaceId().equals(raceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race entry does not belong to this race");
        }

        boolean passed = Boolean.TRUE.equals(check.getPassed());
        entry.setHandicapWeight(request.getHandicapWeight());

        BigDecimal jockeyActualWeight = check.getActualWeight();
        BigDecimal leadWeight = request.getHandicapWeight().subtract(jockeyActualWeight).max(BigDecimal.ZERO);
        BigDecimal carriedWeight = jockeyActualWeight.add(leadWeight);

        entry.setJockeyActualWeight(jockeyActualWeight);
        entry.setLeadWeight(leadWeight);
        entry.setCarriedWeight(carriedWeight);
        entry.setWeightCheckStatus(passed ? "passed" : "failed");
        entry.setEntryStatus(passed ? "ready" : "scratched");
        return entry;
    }

    private void requireWeightCheckPermission(User user, Race race) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
            if (race.getStaff() != null && race.getStaff().getStaffId().equals(staff.getStaffId())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        if (user.getRole() == Role.REFEREE) {
            Referee referee = refereeRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Referee profile not found"));
            if (race.getReferee() != null && race.getReferee().getRefereeId().equals(referee.getRefereeId())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    private void requireEntryCreationPermission(User user, Race race) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
            if (race.getStaff() != null && race.getStaff().getStaffId().equals(staff.getStaffId())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    private void validateRaceAllowsEntryCreation(Race race) {
        if (race == null ||
                race.getStatus() == RaceStatus.CANCELLED ||
                race.getStatus() == RaceStatus.RUNNING ||
                race.getStatus() == RaceStatus.RESULT_PENDING ||
                race.getStatus() == RaceStatus.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is in invalid status: "
                    + (race != null ? race.getStatus() : null));
        }
    }

    private Short resolveInitialGateNumber(Long raceId) {
        if (!raceEntryRepository.existsByRaceRaceIdAndGateNumber(raceId, null)) {
            return null;
        }
        int nextGateNumber = raceEntryRepository.findMaxGateNumberByRaceId(raceId) + 1;
        if (nextGateNumber > Short.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No available gate number");
        }
        return (short) nextGateNumber;
    }

    private void rethrowConflictIfRaceEntryDuplicate(RaceRegistration registration, RaceInvitation invitation,
            Race race) {
        if (raceEntryRepository.existsByRegistrationRegistrationId(registration.getRegistrationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already has an entry");
        }
        if (raceEntryRepository.existsByInvitationInvitationId(invitation.getInvitationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already has an entry");
        }
        if (raceEntryRepository.existsByRaceRaceIdAndJockeyJockeyId(race.getRaceId(),
                invitation.getJockey().getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jockey is already entered in this race");
        }
        if (raceEntryRepository.existsByRaceRaceIdAndHorseHorseId(race.getRaceId(),
                registration.getHorse().getHorseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Horse is already entered in this race");
        }
    }

    public RaceEntryResponse updateStatus(Long id, UpdateStatusRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.STAFF && currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        RaceEntry entry = raceEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));

        entry.setEntryStatus(request.getStatus());

        return toResponse(raceEntryRepository.save(entry));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private AcceptedInvitationCandidateResponse toCandidateResponse(RaceInvitation invitation) {
        RaceRegistration registration = invitation.getRaceRegistration();
        Race race = registration.getRace();
        Horse horse = registration.getHorse();
        Jockey jockey = invitation.getJockey();

        String reason = resolveCandidateBlockReason(invitation);

        return AcceptedInvitationCandidateResponse.builder()
                .invitationId(invitation.getInvitationId())
                .registrationId(registration.getRegistrationId())
                .raceId(race.getRaceId())
                .raceName(race.getRaceName())
                .horseId(horse.getHorseId())
                .horseName(horse.getHorseName())
                .ownerId(horse.getOwner() != null ? horse.getOwner().getUserId() : null)
                .ownerName(horse.getOwner() != null ? horse.getOwner().getFullName() : null)
                .jockeyId(jockey.getJockeyId())
                .jockeyName(jockey.getUser().getFullName())
                .invitationStatus(
                        invitation.getInvitationStatus() != null ? invitation.getInvitationStatus().name() : null)
                .registrationStatus(registration.getStatus() != null ? registration.getStatus().name() : null)
                .canCreateEntry(reason == null)
                .reason(reason)
                .build();
    }

    private String resolveCandidateBlockReason(RaceInvitation invitation) {
        RaceRegistration registration = invitation.getRaceRegistration();
        Race race = registration.getRace();
        Horse horse = registration.getHorse();
        Jockey jockey = invitation.getJockey();

        if (invitation.getInvitationStatus() != RaceInvitationStatus.ACCEPTED) {
            return "Invitation is not ACCEPTED";
        }
        if (registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            return "Registration is not APPROVED";
        }
        if (!jockeyRaceRegistrationRepository.existsByRaceRaceIdAndJockeyJockeyIdAndStatus(
                race.getRaceId(), jockey.getJockeyId(), JockeyRaceRegistrationStatus.REGISTERED)) {
            return "Jockey has not registered for this race";
        }
        if (raceEntryRepository.existsByRegistrationRegistrationId(registration.getRegistrationId())) {
            return "Registration already has an entry";
        }
        if (raceEntryRepository.existsByInvitationInvitationId(invitation.getInvitationId())) {
            return "Invitation already has an entry";
        }
        if (raceEntryRepository.existsByRaceRaceIdAndJockeyJockeyId(race.getRaceId(), jockey.getJockeyId())) {
            return "Jockey is already entered in this race";
        }
        if (raceEntryRepository.existsByRaceRaceIdAndHorseHorseId(race.getRaceId(), horse.getHorseId())) {
            return "Horse is already entered in this race";
        }
        return null;
    }

    private RaceEntryResponse toResponse(RaceEntry entry) {
        return RaceEntryResponse.builder()
                .entryId(entry.getEntryId())
                .raceId(entry.getRace().getRaceId())
                .raceName(entry.getRace().getRaceName())
                .registrationId(entry.getRegistration().getRegistrationId())
                .invitationId(entry.getInvitation() != null ? entry.getInvitation().getInvitationId() : null)
                .horseId(entry.getHorse().getHorseId())
                .horseName(entry.getHorse().getHorseName())
                .jockeyId(entry.getJockey().getJockeyId())
                .jockeyName(entry.getJockey().getUser().getFullName())
                .gateNumber(entry.getGateNumber())
                .handicapWeight(entry.getHandicapWeight())
                .jockeyActualWeight(entry.getJockeyActualWeight())
                .leadWeight(entry.getLeadWeight())
                .carriedWeight(entry.getCarriedWeight())
                .weightCheckStatus(entry.getWeightCheckStatus())
                .weightCheckedBy(entry.getWeightCheckedBy() != null ? entry.getWeightCheckedBy().getRefereeId() : null)
                .weightCheckedByName(
                        entry.getWeightCheckedBy() != null ? entry.getWeightCheckedBy().getUser().getFullName() : null)
                .weightCheckedAt(entry.getWeightCheckedAt())
                .entryStatus(entry.getEntryStatus())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
