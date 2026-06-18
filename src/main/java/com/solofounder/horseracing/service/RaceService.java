package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.race.RaceRequest;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceService {

    private static final String OPEN_STATUS = "registration_open";
    private static final Set<String> VALID_STATUSES = Set.of(
            "draft",
            "scheduled",
            "registration_open",
            "registration_closed",
            "entries_finalized",
            "racecard_published",
            "running",
            "provisional_result",
            "under_review",
            "official_result",
            "closed",
            "cancelled"
    );

    private final RaceRepository raceRepository;
    private final RaceMeetingRepository raceMeetingRepository;
    private final RaceConditionRepository raceConditionRepository;
    private final StaffRepository staffRepository;
    private final RefereeRepository refereeRepository;

    public List<RaceResponse> getAllRaces() {
        return raceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RaceResponse> getOpenRaces() {
        return raceRepository.findByStatus(OPEN_STATUS).stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceResponse getRace(Long raceId) {
        return toResponse(findRace(raceId));
    }

    public RaceResponse createRace(RaceRequest request) {
        RaceMeeting raceMeeting = findRaceMeeting(request.getMeetingId());
        RaceCondition raceCondition = findRaceCondition(request.getConditionId());
        Staff staff = findStaffIfProvided(request.getStaffId());
        Referee referee = findRefereeIfProvided(request.getRefereeId());
        Race race = Race.builder()
                .raceMeeting(raceMeeting)
                .raceCondition(raceCondition)
                .staff(staff)
                .referee(referee)
                .raceName(normalizeRequired(request.getRaceName(), "Race name is required"))
                .raceNo(validateRaceNo(request.getRaceNo()))
                .scheduledTime(request.getScheduledTime())
                .status(normalizeStatus(request.getStatus()))
                .build();
        return toResponse(raceRepository.save(race));
    }

    public RaceResponse updateRace(Long raceId, RaceRequest request) {
        Race race = findRace(raceId);
        race.setRaceMeeting(findRaceMeeting(request.getMeetingId()));
        race.setRaceCondition(findRaceCondition(request.getConditionId()));
        race.setStaff(findStaffIfProvided(request.getStaffId()));
        race.setReferee(findRefereeIfProvided(request.getRefereeId()));
        race.setRaceName(normalizeRequired(request.getRaceName(), "Race name is required"));
        race.setRaceNo(validateRaceNo(request.getRaceNo()));
        race.setScheduledTime(request.getScheduledTime());
        race.setStatus(normalizeStatus(request.getStatus()));
        return toResponse(raceRepository.save(race));
    }

    public void deleteRace(Long raceId) {
        Race race = findRace(raceId);
        if (raceRepository.countRaceRegistrations(raceId) > 0
                || raceRepository.countRaceEntries(raceId) > 0
                || raceRepository.countRaceResults(raceId) > 0) {
            throw new IllegalStateException("Cannot delete race with existing registration, entry, or result");
        }
        raceRepository.delete(race);
    }

    private Race findRace(Long raceId) {
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
    }

    private RaceMeeting findRaceMeeting(Long meetingId) {
        return raceMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race meeting not found"));
    }

    private RaceCondition findRaceCondition(Long conditionId) {
        return raceConditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race condition not found"));
    }

    private Staff findStaffIfProvided(Long staffId) {
        if (staffId == null) {
            return null;
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        if (staff.getUser().getRole() != Role.STAFF) {
            throw new IllegalArgumentException("Staff profile must belong to user role STAFF");
        }
        return staff;
    }

    private Referee findRefereeIfProvided(Long refereeId) {
        if (refereeId == null) {
            return null;
        }
        Referee referee = refereeRepository.findById(refereeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referee not found"));
        if (referee.getUser().getRole() != Role.REFEREE) {
            throw new IllegalArgumentException("Referee profile must belong to user role REFEREE");
        }
        return referee;
    }

    private Short validateRaceNo(Short raceNo) {
        if (raceNo != null && raceNo <= 0) {
            throw new IllegalArgumentException("Race number must be greater than 0");
        }
        return raceNo;
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "draft";
        }
        normalized = normalized.toLowerCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Race status is invalid");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private RaceResponse toResponse(Race race) {
        Staff staff = race.getStaff();
        Referee referee = race.getReferee();
        return RaceResponse.builder()
                .raceId(race.getRaceId())
                .meetingId(race.getRaceMeeting().getMeetingId())
                .conditionId(race.getRaceCondition().getConditionId())
                .conditionName(race.getRaceCondition().getConditionName())
                .staffId(staff == null ? null : staff.getStaffId())
                .staffName(staff == null ? null : staff.getUser().getFullName())
                .refereeId(referee == null ? null : referee.getRefereeId())
                .refereeName(referee == null ? null : referee.getUser().getFullName())
                .raceName(race.getRaceName())
                .raceNo(race.getRaceNo())
                .scheduledTime(race.getScheduledTime())
                .status(race.getStatus())
                .createdAt(race.getCreatedAt())
                .updatedAt(race.getUpdatedAt())
                .build();
    }
}
