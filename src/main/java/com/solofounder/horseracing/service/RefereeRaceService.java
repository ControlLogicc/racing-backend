package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.entry.RaceEntryResponse;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.UserRepository;
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
                .jockeyId(entry.getJockey().getJockeyId())
                .jockeyName(entry.getJockey().getUser().getFullName())
                .gateNumber(entry.getGateNumber())
                .drawNumber(entry.getDrawNumber())
                .handicapWeight(entry.getHandicapWeight())
                .actualWeight(entry.getActualWeight())
                .weightCheckStatus(entry.getWeightCheckStatus())
                .entryStatus(entry.getEntryStatus())
                .confirmedByStaffId(entry.getConfirmedByStaff() != null ? entry.getConfirmedByStaff().getStaffId() : null)
                .confirmedByStaffName(entry.getConfirmedByStaff() != null ? entry.getConfirmedByStaff().getUser().getFullName() : null)
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
