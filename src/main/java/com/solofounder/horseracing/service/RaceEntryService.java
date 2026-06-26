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
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceEntryService {

    private static final String ENTRY_DECLARED = "DECLARED";
    private static final String ENTRY_PASSED = "PASSED";
    private static final String ENTRY_FAILED = "FAILED";
    private static final String ENTRY_WITHDRAWN = "WITHDRAWN";
    private static final BigDecimal WEIGHT_TOLERANCE_KG = new BigDecimal("0.50");

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

        Staff confirmedBy = null;
        if (currentUser.getRole() == Role.STAFF) {
            confirmedBy = staffRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found"));
        }

        RaceRegistration registration = null;
        RaceInvitation invitation = null;

        if (request.getInvitationId() != null) {
            invitation = raceInvitationRepository.findById(request.getInvitationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
            registration = invitation.getRaceRegistration();
        } else {
            if (request.getRegistrationId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration ID is required");
            }
            registration = raceRegistrationRepository.findById(request.getRegistrationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        }

        if (registration == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found");
        }

        if (raceEntryRepository.existsByRegistrationRegistrationId(registration.getRegistrationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already has an entry");
        }

        if (request.getInvitationId() == null) {
            if (request.getJockeyId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jockey ID is required");
            }
            invitation = raceInvitationRepository.findByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatus(
                    registration.getRegistrationId(),
                    request.getJockeyId(),
                    RaceInvitationStatus.ACCEPTED
            ).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No ACCEPTED invitation found for this jockey and registration"));
        }
        if (registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not APPROVED");
        }

        if (invitation.getInvitationStatus() != RaceInvitationStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is not ACCEPTED");
        }

        Race race = registration.getRace();
        requireEntryCreationPermission(currentUser, race);
        validateRaceAllowsEntryCreation(race);

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

        Short gateNum = request.getGateNumber() != null ? request.getGateNumber() : resolveInitialGateNumber(race.getRaceId());
        if (gateNum != null && raceEntryRepository.existsByRaceRaceIdAndGateNumber(race.getRaceId(), gateNum)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gate number is already taken in this race");
        }

        if (!jockeyRaceRegistrationRepository.existsByRaceRaceIdAndJockeyJockeyIdAndStatus(
                race.getRaceId(),
                invitation.getJockey().getJockeyId(),
                JockeyRaceRegistrationStatus.REGISTERED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jockey has not registered for this race");
        }

        RaceEntry entry = RaceEntry.builder()
                .race(race)
                .registration(registration)
                .invitation(invitation)
                .horse(registration.getHorse())
                .jockey(invitation.getJockey())
                .gateNumber(gateNum)
                .handicapWeight(request.getHandicapWeight())
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

    public RaceEntryResponse refereePreCheck(Long id, RefereePreCheckRequest request) {
        User currentUser = getCurrentUser();
        RaceEntry entry = raceEntryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));
        Race race = entry.getRace();
        requireRefereePreCheckPermission(currentUser, race);

        if (!isStatus(entry.getEntryStatus(), ENTRY_DECLARED)
                && !isStatus(entry.getEntryStatus(), ENTRY_FAILED)
                && !isStatus(entry.getEntryStatus(), ENTRY_PASSED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DECLARED, PASSED, or FAILED entries can be pre-checked");
        }

        if (request.getHandicapWeight() != null) {
            entry.setHandicapWeight(request.getHandicapWeight());
        }
        if (request.getActualWeight() != null) {
            entry.setJockeyActualWeight(request.getActualWeight());
        }
        if (request.getNote() != null) {
            entry.setPreCheckNote(request.getNote().trim().isEmpty() ? null : request.getNote().trim());
        }

        if (entry.getJockeyActualWeight() == null || entry.getHandicapWeight() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Actual weight and handicap weight are required for pre-check");
        }

        BigDecimal diff = entry.getJockeyActualWeight().subtract(entry.getHandicapWeight()).abs().setScale(2, RoundingMode.HALF_UP);
        String status = diff.compareTo(WEIGHT_TOLERANCE_KG) <= 0 ? ENTRY_PASSED : ENTRY_FAILED;
        entry.setWeightCheckStatus(status);
        entry.setEntryStatus(status);

        return toResponse(raceEntryRepository.save(entry));
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

        entry.setHandicapWeight(request.getHandicapWeight());
        entry.setPreCheckNote(check.getNote() != null && !check.getNote().trim().isEmpty() ? check.getNote().trim() : null);

        BigDecimal jockeyActualWeight = check.getActualWeight();
        BigDecimal leadWeight = request.getHandicapWeight().subtract(jockeyActualWeight).max(BigDecimal.ZERO);
        BigDecimal carriedWeight = jockeyActualWeight.add(leadWeight);

        entry.setJockeyActualWeight(jockeyActualWeight);
        entry.setLeadWeight(leadWeight);
        entry.setCarriedWeight(carriedWeight);
        boolean passed = check.getPassed();
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

    private void requireRefereePreCheckPermission(User user, Race race) {
        if (user.getRole() == Role.ADMIN) {
            return;
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
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        RaceEntry entry = raceEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));

        String requestedStatus = request.getStatus().trim();
        if (isStatus(requestedStatus, ENTRY_WITHDRAWN) && !isStatus(entry.getEntryStatus(), ENTRY_FAILED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only FAILED entries can be withdrawn");
        }
        entry.setEntryStatus(requestedStatus);

        return toResponse(raceEntryRepository.save(entry));
    }

    public List<RaceEntryResponse> randomizeGates(Long raceId) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.STAFF && currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        List<RaceEntry> entries = raceEntryRepository.findByRaceRaceId(raceId).stream()
                .filter(e -> !isStatus(e.getEntryStatus(), ENTRY_WITHDRAWN))
                .toList();

        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active entries found for this race");
        }

        List<Integer> gates = new java.util.ArrayList<>();
        for (int i = 1; i <= entries.size(); i++) {
            gates.add(i);
        }
        java.util.Collections.shuffle(gates);

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setGateNumber((short) (int) gates.get(i));
        }

        return raceEntryRepository.saveAll(entries).stream()
                .map(this::toResponse)
                .toList();
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
                .preCheckNote(entry.getPreCheckNote())
                .weightCheckedBy(entry.getWeightCheckedBy() != null ? entry.getWeightCheckedBy().getRefereeId() : null)
                .weightCheckedByName(
                        entry.getWeightCheckedBy() != null ? entry.getWeightCheckedBy().getUser().getFullName() : null)
                .weightCheckedAt(entry.getWeightCheckedAt())
                .entryStatus(entry.getEntryStatus())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .scheduledTime(entry.getRace().getScheduledTime())
                .build();
    }

    private String calculateWeightCheckStatus(BigDecimal actualWeight, BigDecimal handicapWeight) {
        if (actualWeight == null || handicapWeight == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Actual weight and handicap weight are required for pre-check");
        }
        BigDecimal diff = actualWeight.subtract(handicapWeight).abs().setScale(2, RoundingMode.HALF_UP);
        return diff.compareTo(WEIGHT_TOLERANCE_KG) <= 0 ? ENTRY_PASSED : ENTRY_FAILED;
    }

    private boolean isStatus(String actual, String expected) {
        return actual != null && actual.equalsIgnoreCase(expected);
    }
}
