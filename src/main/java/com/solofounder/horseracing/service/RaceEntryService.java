package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.entry.AcceptedInvitationCandidateResponse;
import com.solofounder.horseracing.dto.entry.BatchWeightCheckRequest;
import com.solofounder.horseracing.dto.entry.CreateRaceEntryRequest;
import com.solofounder.horseracing.dto.entry.RaceEntryResponse;
import com.solofounder.horseracing.dto.entry.RefereePreCheckRequest;
import com.solofounder.horseracing.dto.entry.UpdateStatusRequest;
import com.solofounder.horseracing.dto.entry.UpdateWeightRequest;
import com.solofounder.horseracing.dto.entry.WeightCheckItemRequest;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceInvitation;
import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.JockeyRaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.JockeyRaceRegistrationRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceInvitationRepository;
import com.solofounder.horseracing.repository.RaceRegistrationRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

        validateRequestedEntryIsNotAlreadyCreated(request);
        RaceInvitation invitation = resolveAcceptedInvitation(request);
        RaceRegistration registration = invitation.getRaceRegistration();
        if (registration.getStatus() != RaceRegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is not APPROVED");
        }

        Race race = registration.getRace();
        requireEntryCreationPermission(currentUser, race);
        validateRaceAllowsEntryCreation(race);

        validateEntryDoesNotExist(registration, invitation, race);

        BigDecimal handicapWeight = request.getHandicapWeight();
        if (handicapWeight == null) {
            handicapWeight = calculateHKJCHandicapWeight(race, registration.getHorse());
        }

        RaceEntry entry = RaceEntry.builder()
                .race(race)
                .registration(registration)
                .invitation(invitation)
                .horse(registration.getHorse())
                .jockey(invitation.getJockey())
                .gateNumber(resolveInitialGateNumber(race.getRaceId(), request.getGateNumber()))
                .handicapWeight(handicapWeight)
                .entryStatus(ENTRY_DECLARED)
                .build();

        RaceEntry savedEntry = raceEntryRepository.saveAndFlush(entry);

        invitation.setInvitationStatus(RaceInvitationStatus.USED);
        raceInvitationRepository.saveAndFlush(invitation);

        return toResponse(savedEntry);
    }

    private void validateRequestedEntryIsNotAlreadyCreated(CreateRaceEntryRequest request) {
        if (request.getRegistrationId() != null
                && raceEntryRepository.existsByRegistrationRegistrationId(request.getRegistrationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already has an entry");
        }
        if (request.getInvitationId() != null
                && raceEntryRepository.existsByInvitationInvitationId(request.getInvitationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already has an entry");
        }
    }

    private RaceInvitation resolveAcceptedInvitation(CreateRaceEntryRequest request) {
        if (request.getInvitationId() != null) {
            RaceInvitation invitation = raceInvitationRepository.findByIdWithEntryDetails(request.getInvitationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
            if (invitation.getInvitationStatus() != RaceInvitationStatus.ACCEPTED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is not ACCEPTED");
            }
            return invitation;
        }

        if (request.getRegistrationId() == null || request.getJockeyId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration ID and jockey ID are required");
        }

        return raceInvitationRepository
                .findByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatus(
                        request.getRegistrationId(),
                        request.getJockeyId(),
                        RaceInvitationStatus.ACCEPTED
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No ACCEPTED invitation found for this jockey and registration"
                ));
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DECLARED, PASSED, or FAILED entries can be pre-checked");
        }

        if (request.getHandicapWeight() != null) {
            entry.setHandicapWeight(request.getHandicapWeight());
        }
        
        // Cập nhật giá trị cân nặng
        applyWeightValues(entry, request.getActualWeight(), request.getNote(), currentUser);
        
        // Nếu client truyền trực tiếp leadWeight hoặc carriedWeight, ta ưu tiên sử dụng để đồng bộ với UI
        if (request.getLeadWeight() != null) {
            entry.setLeadWeight(request.getLeadWeight());
        }
        if (request.getCarriedWeight() != null) {
            entry.setCarriedWeight(request.getCarriedWeight());
            // Cập nhật lại status theo carried weight thực tế truyền lên
            String status = calculateWeightCheckStatus(entry.getCarriedWeight(), entry.getHandicapWeight());
            entry.setWeightCheckStatus(status);
            entry.setEntryStatus(status);
        }

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
                .map(check -> applyWeightCheck(raceId, request, check, currentUser))
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

        BigDecimal actualWeight = request.getActualWeight();
        applyCarriedWeight(entry, actualWeight);
        entry.setWeightCheckStatus(normalizeEntryStatus(request.getWeightCheckStatus()));

        return toResponse(raceEntryRepository.save(entry));
    }

    public RaceEntryResponse updateStatus(Long id, UpdateStatusRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        RaceEntry entry = raceEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));

        String requestedStatus = normalizeEntryStatus(request.getStatus());
        if (ENTRY_WITHDRAWN.equals(requestedStatus) && !isStatus(entry.getEntryStatus(), ENTRY_FAILED)) {
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
                .filter(entry -> !isStatus(entry.getEntryStatus(), ENTRY_WITHDRAWN))
                .toList();
        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active entries found for this race");
        }

        List<Integer> gates = new ArrayList<>();
        for (int i = 1; i <= entries.size(); i++) {
            gates.add(i);
        }
        Collections.shuffle(gates);

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setGateNumber((short) gates.get(i).intValue());
        }

        return raceEntryRepository.saveAll(entries).stream()
                .map(this::toResponse)
                .toList();
    }

    private RaceEntry applyWeightCheck(Long raceId, BatchWeightCheckRequest request, WeightCheckItemRequest check,
            User currentUser) {
        RaceEntry entry = raceEntryRepository.findByIdWithDetails(check.getEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));

        if (!entry.getRace().getRaceId().equals(raceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race entry does not belong to this race");
        }

        entry.setHandicapWeight(request.getHandicapWeight());
        applyWeightValues(entry, check.getActualWeight(), check.getNote(), currentUser);
        return entry;
    }

    private void applyWeightValues(RaceEntry entry, BigDecimal actualWeight, String note, User currentUser) {
        if (entry.getHandicapWeight() == null || actualWeight == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Actual weight and handicap weight are required for pre-check");
        }

        applyCarriedWeight(entry, actualWeight);
        if (note != null) {
            entry.setPreCheckNote(note.trim().isEmpty() ? null : note.trim());
        }

        String status = calculateWeightCheckStatus(entry.getCarriedWeight(), entry.getHandicapWeight());
        entry.setWeightCheckStatus(status);
        entry.setEntryStatus(status);
        if (currentUser.getRole() == Role.REFEREE) {
            Referee referee = refereeRepository.findByUserUserId(currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Referee profile not found"));
            entry.setWeightCheckedBy(referee);
            entry.setWeightCheckedAt(LocalDateTime.now());
        }
    }

    private void applyCarriedWeight(RaceEntry entry, BigDecimal actualWeight) {
        BigDecimal leadWeight = BigDecimal.ZERO;
        BigDecimal carriedWeight = actualWeight;
        if (entry.getHandicapWeight() != null) {
            leadWeight = entry.getHandicapWeight().subtract(actualWeight).max(BigDecimal.ZERO);
            carriedWeight = actualWeight.add(leadWeight);
        }
        entry.setJockeyActualWeight(actualWeight);
        entry.setLeadWeight(leadWeight);
        entry.setCarriedWeight(carriedWeight);
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
        }
        if (user.getRole() == Role.REFEREE) {
            Referee referee = refereeRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Referee profile not found"));
            if (race.getReferee() != null && race.getReferee().getRefereeId().equals(referee.getRefereeId())) {
                return;
            }
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
        if (race == null
                || race.getStatus() == RaceStatus.CANCELLED
                || race.getStatus() == RaceStatus.RUNNING
                || race.getStatus() == RaceStatus.RESULT_PENDING
                || race.getStatus() == RaceStatus.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Race is in invalid status: " + (race != null ? race.getStatus() : null));
        }
    }

    private void validateJockeyRegisteredForRace(Long raceId, Long jockeyId) {
        if (!jockeyRaceRegistrationRepository.existsByRaceRaceIdAndJockeyJockeyIdAndStatus(
                raceId,
                jockeyId,
                JockeyRaceRegistrationStatus.REGISTERED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jockey has not registered for this race");
        }
    }

    private void validateEntryDoesNotExist(RaceRegistration registration, RaceInvitation invitation, Race race) {
        if (raceEntryRepository.existsByRegistrationRegistrationId(registration.getRegistrationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already has an entry");
        }
        if (raceEntryRepository.existsByInvitationInvitationId(invitation.getInvitationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already has an entry");
        }
        if (raceEntryRepository.existsByRaceRaceIdAndJockeyJockeyId(
                race.getRaceId(),
                invitation.getJockey().getJockeyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jockey is already entered in this race");
        }
        if (raceEntryRepository.existsByRaceRaceIdAndHorseHorseId(
                race.getRaceId(),
                registration.getHorse().getHorseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Horse is already entered in this race");
        }
    }

    private Short resolveInitialGateNumber(Long raceId, Short requestedGateNumber) {
        if (requestedGateNumber != null) {
            if (raceEntryRepository.existsByRaceRaceIdAndGateNumber(raceId, requestedGateNumber)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Gate number is already occupied in this race");
            }
            return requestedGateNumber;
        }
        return null;
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
                .invitationStatus(invitation.getInvitationStatus() != null ? invitation.getInvitationStatus().name() : null)
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
                race.getRaceId(),
                jockey.getJockeyId(),
                JockeyRaceRegistrationStatus.REGISTERED)) {
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
                .actualWeight(entry.getJockeyActualWeight())
                .jockeyActualWeight(entry.getJockeyActualWeight())
                .leadWeight(entry.getLeadWeight())
                .carriedWeight(entry.getCarriedWeight())
                .weightCheckStatus(entry.getWeightCheckStatus())
                .preCheckNote(entry.getPreCheckNote())
                .weightCheckedBy(entry.getWeightCheckedBy() != null ? entry.getWeightCheckedBy().getRefereeId() : null)
                .weightCheckedByName(entry.getWeightCheckedBy() != null
                        ? entry.getWeightCheckedBy().getUser().getFullName()
                        : null)
                .weightCheckedAt(entry.getWeightCheckedAt())
                .entryStatus(entry.getEntryStatus())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .scheduledTime(entry.getRace().getScheduledTime())
                .build();
    }

    private String calculateWeightCheckStatus(BigDecimal carriedWeight, BigDecimal handicapWeight) {
        if (carriedWeight == null || handicapWeight == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Carried weight and handicap weight are required for pre-check");
        }
        BigDecimal diff = carriedWeight.subtract(handicapWeight).abs().setScale(2, RoundingMode.HALF_UP);
        return diff.compareTo(WEIGHT_TOLERANCE_KG) <= 0 ? ENTRY_PASSED : ENTRY_FAILED;
    }

    private String normalizeEntryStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    private boolean isStatus(String actual, String expected) {
        return actual != null && actual.equalsIgnoreCase(expected);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    // HKJC-style: tính tương đối trong field của race
    // handicap(horse) = topWeight(class) - (topRating - horse.rating) × 0.454 kg/lb
    // topWeight lấy từ horseClass của con ngựa có rating cao nhất trong field
    // Min weight = 51.3 kg (113 lbs)
    private BigDecimal calculateHKJCHandicapWeight(Race race, Horse thisHorse) {
        BigDecimal minWeightKg = new BigDecimal("51.3"); // 113 lbs
        BigDecimal lbToKg = new BigDecimal("0.454");

        List<RaceEntry> existingEntries = raceEntryRepository.findByRaceRaceId(race.getRaceId());

        // Tìm ngựa có rating cao nhất trong field hiện tại
        RaceEntry topEntry = existingEntries.stream()
                .filter(e -> e.getHorse() != null)
                .max((a, b) -> {
                    BigDecimal ra = a.getHorse().getCurrentScore() != null ? a.getHorse().getCurrentScore() : BigDecimal.ZERO;
                    BigDecimal rb = b.getHorse().getCurrentScore() != null ? b.getHorse().getCurrentScore() : BigDecimal.ZERO;
                    return ra.compareTo(rb);
                })
                .orElse(null);

        BigDecimal topRating = BigDecimal.ZERO;
        short topClass = 5;
        if (topEntry != null && topEntry.getHorse() != null) {
            topRating = topEntry.getHorse().getCurrentScore() != null ? topEntry.getHorse().getCurrentScore() : BigDecimal.ZERO;
            topClass = topEntry.getHorse().getHorseClass() != null ? topEntry.getHorse().getHorseClass() : 5;
        }

        BigDecimal thisRating = thisHorse != null && thisHorse.getCurrentScore() != null
                ? thisHorse.getCurrentScore() : BigDecimal.ZERO;

        // Nếu ngựa này là top (rating >= field hiện tại) → dùng class của chính nó làm topWeight
        if (thisRating.compareTo(topRating) >= 0) {
            short thisClass = thisHorse != null && thisHorse.getHorseClass() != null ? thisHorse.getHorseClass() : 5;
            return resolveTopWeight(thisClass);
        }

        // topWeight dựa vào class của ngựa có rating cao nhất trong field
        BigDecimal topWeightKg = resolveTopWeight(topClass);

        // handicap = topWeight - (topRating - thisRating) × 0.454
        BigDecimal diff = topRating.subtract(thisRating);
        BigDecimal handicap = topWeightKg.subtract(diff.multiply(lbToKg))
                .setScale(1, RoundingMode.HALF_UP);

        return handicap.max(minWeightKg);
    }

    // Top weight (kg) theo horseClass (1=cao nhất, 5=thấp nhất)
    private BigDecimal resolveTopWeight(short horseClass) {
        switch (horseClass) {
            case 1: return new BigDecimal("60.3"); // 133 lbs
            case 2: return new BigDecimal("57.2"); // 126 lbs
            case 3: return new BigDecimal("55.8"); // 123 lbs
            case 4: return new BigDecimal("53.5"); // 118 lbs
            default: return new BigDecimal("51.3"); // 113 lbs (Class 5)
        }
    }
}
