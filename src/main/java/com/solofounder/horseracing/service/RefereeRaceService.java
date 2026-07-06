package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.entry.JockeyWeightCheckRequest;
import com.solofounder.horseracing.dto.entry.JockeyWeightCheckResponse;
import com.solofounder.horseracing.dto.entry.RaceEntryResponse;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.model.Race;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.UserRepository;
import com.solofounder.horseracing.util.PreRaceWeightCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefereeRaceService {

    private final RefereeRepository refereeRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final UserRepository userRepository;

    public List<RaceResponse> getCurrentRefereeRaces() {
        Referee referee = getCurrentReferee();
        return raceRepository.findByRefereeRefereeIdWithDetails(referee.getRefereeId()).stream()
                .map(this::toRaceResponse)
                .toList();
    }

    public List<RaceEntryResponse> getEntriesForAssignedRace(Long raceId) {
        Referee referee = getCurrentReferee();
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        if (race.getReferee() == null || !race.getReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return raceEntryRepository.findByRaceRaceIdWithDetails(raceId).stream()
                .map(this::toEntryResponse)
                .toList();
    }

    @Transactional
    public JockeyWeightCheckResponse checkJockeyWeight(Long entryId, JockeyWeightCheckRequest request) {
        Referee referee = getCurrentReferee();
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));

        Race race = entry.getRace();
        if (race.getReferee() == null || !race.getReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        BigDecimal handicapWeight = entry.getHandicapWeight();
        BigDecimal jockeyActualWeight = request.getJockeyActualWeight();
        PreRaceWeightCheck.Result result = PreRaceWeightCheck.evaluate(jockeyActualWeight, handicapWeight);

        entry.setJockeyActualWeight(jockeyActualWeight);
        entry.setLeadWeight(BigDecimal.ZERO);
        entry.setCarriedWeight(result.carriedWeight());
        entry.setWeightCheckStatus(result.weightCheckStatus());
        entry.setEntryStatus(result.entryStatus());
        entry.setPreCheckNote(weightCheckNote(entry.getPreCheckNote(), result));
        entry.setWeightCheckedBy(referee);
        entry.setWeightCheckedAt(LocalDateTime.now());

        raceEntryRepository.save(entry);

        return JockeyWeightCheckResponse.builder()
                .entryId(entry.getEntryId())
                .handicapWeight(handicapWeight)
                .actualWeight(jockeyActualWeight)
                .jockeyActualWeight(jockeyActualWeight)
                .leadWeight(BigDecimal.ZERO)
                .carriedWeight(result.carriedWeight())
                .overweightAmount(result.overweightAmount())
                .weightCheckStatus(result.weightCheckStatus())
                .entryStatus(result.entryStatus())
                .horseName(entry.getHorse().getHorseName())
                .jockeyName(entry.getJockey().getUser().getFullName())
                .note(entry.getPreCheckNote())
                .build();
    }

    private String weightCheckNote(String existingNote, PreRaceWeightCheck.Result result) {
        String automaticNote = null;
        if (result.isOverweight() && result.isPassed()) {
            automaticNote = "Overweight declared: +" + result.overweightAmount().toPlainString() + " kg";
        } else if (!result.isPassed()) {
            automaticNote = "Actual carried weight exceeds allowed overweight tolerance.";
        }
        if (automaticNote == null) {
            return existingNote;
        }
        return existingNote == null || existingNote.isBlank()
                ? automaticNote
                : existingNote.trim() + " | " + automaticNote;
    }

    private Referee getCurrentReferee() {
        User user = getCurrentUser();
        if (user.getRole() != Role.REFEREE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return refereeRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referee profile not found"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private RaceResponse toRaceResponse(Race race) {
        Staff staff = race.getStaff();
        Referee referee = race.getReferee();
        return RaceResponse.builder()
                .raceId(race.getRaceId())
                .meetingId(race.getRaceMeeting().getMeetingId())
                .meetingName(race.getRaceMeeting().getMeetingName())
                .meetingDate(race.getRaceMeeting().getMeetingDate())
                .racecourseId(race.getRaceMeeting().getRacecourse().getRacecourseId())
                .racecourseName(race.getRaceMeeting().getRacecourse().getRacecourseName())
                .conditionId(race.getRaceCondition().getConditionId())
                .conditionName(race.getRaceCondition().getConditionName())
                .distanceMeters(race.getRaceCondition().getDistance())
                .trackType(race.getRaceCondition().getTrackType())
                .classRequirement(race.getRaceCondition().getClassRequirement())
                .minEntries(race.getRaceCondition().getMinEntries())
                .maxEntries(race.getRaceCondition().getMaxEntries())
                .staffId(staff == null ? null : staff.getStaffId())
                .staffName(staff == null ? null : staff.getUser().getFullName())
                .refereeId(referee == null ? null : referee.getRefereeId())
                .refereeName(referee == null ? null : referee.getUser().getFullName())
                .raceName(race.getRaceName())
                .raceNo(race.getRaceNo())
                .scheduledTime(race.getScheduledTime())
                .registrationOpenAt(race.getRegistrationOpenAt())
                .registrationCloseAt(race.getRegistrationCloseAt())
                .status(race.getStatus() != null ? race.getStatus().name() : null)
                .createdAt(race.getCreatedAt())
                .updatedAt(race.getUpdatedAt())
                .build();
    }

    private RaceEntryResponse toEntryResponse(RaceEntry entry) {
        return RaceEntryResponse.builder()
                .entryId(entry.getEntryId())
                .raceId(entry.getRace().getRaceId())
                .raceName(entry.getRace().getRaceName())
                .registrationId(entry.getRegistration().getRegistrationId())
                .invitationId(entry.getInvitation() != null ? entry.getInvitation().getInvitationId() : null)
                .horseId(entry.getHorse().getHorseId())
                .horseName(entry.getHorse().getHorseName())
                .currentScore(entry.getHorse().getCurrentScore())
                .horseClass(entry.getHorse().getHorseClass())
                .ratingVerified(entry.getHorse().isRatingVerified())
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
