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

import com.solofounder.horseracing.model.enums.RaceStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceService {

    private final RaceRepository raceRepository;
    private final RaceMeetingRepository raceMeetingRepository;
    private final RaceConditionRepository raceConditionRepository;
    private final StaffRepository staffRepository;
    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;

    public List<RaceResponse> getAllRaces() {
        return raceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public boolean isOpenForRegistration(Race race, LocalDateTime now) {
        return race != null
                && race.getStatus() == RaceStatus.OPEN_FOR_ENTRY
                && race.getRegistrationOpenAt() != null
                && race.getRegistrationCloseAt() != null
                && !now.isBefore(race.getRegistrationOpenAt())
                && !now.isAfter(race.getRegistrationCloseAt());
    }

    public List<RaceResponse> getOpenRaces() {
        LocalDateTime now = LocalDateTime.now();
        return raceRepository.findByStatus(RaceStatus.OPEN_FOR_ENTRY).stream()
                .filter(r -> isOpenForRegistration(r, now))
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

        if (request.getScheduledTime() == null) {
            throw new IllegalArgumentException("Scheduled time is required");
        }
        if (!request.getScheduledTime().toLocalDate().equals(raceMeeting.getMeetingDate())) {
            throw new IllegalArgumentException("Race scheduled date must match meeting date");
        }

        if (request.getRegistrationOpenAt() == null || request.getRegistrationCloseAt() == null) {
            throw new IllegalArgumentException("Registration open and close times are required");
        }
        if (!request.getRegistrationOpenAt().isBefore(request.getRegistrationCloseAt())) {
            throw new IllegalArgumentException("Registration open time must be before registration close time");
        }
        if (!request.getRegistrationCloseAt().isBefore(request.getScheduledTime())) {
            throw new IllegalArgumentException("Registration close time must be before race scheduled time");
        }

        Race race = Race.builder()
                .raceMeeting(raceMeeting)
                .raceCondition(raceCondition)
                .staff(staff)
                .referee(referee)
                .raceName(normalizeRequired(request.getRaceName(), "Race name is required"))
                .raceNo(validateRaceNo(request.getRaceNo()))
                .scheduledTime(request.getScheduledTime())
                .registrationOpenAt(request.getRegistrationOpenAt())
                .registrationCloseAt(request.getRegistrationCloseAt())
                .status(RaceStatus.DRAFT) // Default to DRAFT
                .build();
        return toResponse(raceRepository.save(race));
    }

    public RaceResponse updateRace(Long raceId, RaceRequest request) {
        Race race = findRace(raceId);
        RaceMeeting raceMeeting = findRaceMeeting(request.getMeetingId());

        if (request.getScheduledTime() == null) {
            throw new IllegalArgumentException("Scheduled time is required");
        }
        if (!request.getScheduledTime().toLocalDate().equals(raceMeeting.getMeetingDate())) {
            throw new IllegalArgumentException("Race scheduled date must match meeting date");
        }

        if (request.getRegistrationOpenAt() == null || request.getRegistrationCloseAt() == null) {
            throw new IllegalArgumentException("Registration open and close times are required");
        }
        if (!request.getRegistrationOpenAt().isBefore(request.getRegistrationCloseAt())) {
            throw new IllegalArgumentException("Registration open time must be before registration close time");
        }
        if (!request.getRegistrationCloseAt().isBefore(request.getScheduledTime())) {
            throw new IllegalArgumentException("Registration close time must be before race scheduled time");
        }

        race.setRaceMeeting(raceMeeting);
        race.setRaceCondition(findRaceCondition(request.getConditionId()));
        race.setStaff(findStaffIfProvided(request.getStaffId()));
        race.setReferee(findRefereeIfProvided(request.getRefereeId()));
        race.setRaceName(normalizeRequired(request.getRaceName(), "Race name is required"));
        race.setRaceNo(validateRaceNo(request.getRaceNo()));
        race.setScheduledTime(request.getScheduledTime());
        race.setRegistrationOpenAt(request.getRegistrationOpenAt());
        race.setRegistrationCloseAt(request.getRegistrationCloseAt());

        if (request.getStatus() != null) {
            try {
                race.setStatus(parseRaceStatus(request.getStatus()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Race status is invalid");
            }
        }
        return toResponse(raceRepository.save(race));
    }

    public RaceResponse assignStaff(Long raceId, Long staffId) {
        Race race = findRace(raceId);
        if (staffId == null) {
            throw new IllegalArgumentException("Staff id is required");
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        if (staff.getUser().getRole() != Role.STAFF) {
            throw new IllegalArgumentException("Staff profile must belong to user role STAFF");
        }
        race.setStaff(staff);
        return toResponse(raceRepository.save(race));
    }

    public RaceResponse updateRaceStatus(Long raceId, String newStatusStr) {
        Race race = findRace(raceId);
        RaceStatus newStatus;
        try {
            newStatus = parseRaceStatus(newStatusStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid race status value");
        }

        // Authorization & Ownership check
        User user = getCurrentUser();
        if (user.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"));
            if (race.getStaff() == null || !race.getStaff().getStaffId().equals(staff.getStaffId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
            }
        } else if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        RaceStatus oldStatus = race.getStatus();

        // Allowed transitions validation
        boolean allowed = false;
        if (oldStatus == RaceStatus.DRAFT) {
            allowed = (newStatus == RaceStatus.SCHEDULED || newStatus == RaceStatus.CANCELLED);
        } else if (oldStatus == RaceStatus.SCHEDULED) {
            allowed = (newStatus == RaceStatus.OPEN_FOR_ENTRY || newStatus == RaceStatus.CANCELLED);
        } else if (oldStatus == RaceStatus.OPEN_FOR_ENTRY) {
            allowed = (newStatus == RaceStatus.CLOSED_FOR_ENTRY || newStatus == RaceStatus.CANCELLED);
        } else if (oldStatus == RaceStatus.CLOSED_FOR_ENTRY) {
            allowed = (newStatus == RaceStatus.RUNNING || newStatus == RaceStatus.CANCELLED);
        } else if (oldStatus == RaceStatus.RUNNING) {
            allowed = (newStatus == RaceStatus.RESULT_PENDING);
        } else if (oldStatus == RaceStatus.RESULT_PENDING) {
            allowed = (newStatus == RaceStatus.OFFICIAL);
        }

        if (!allowed) {
            throw new IllegalArgumentException("Race status cannot change from " + oldStatus + " to " + newStatus);
        }

        // Validation before opening: SCHEDULED -> OPEN_FOR_ENTRY
        if (newStatus == RaceStatus.OPEN_FOR_ENTRY) {
            if (race.getRegistrationOpenAt() == null || race.getRegistrationCloseAt() == null) {
                throw new IllegalArgumentException("Registration dates are not set");
            }
            if (!race.getRegistrationOpenAt().isBefore(race.getRegistrationCloseAt())) {
                throw new IllegalArgumentException("Registration open time must be before registration close time");
            }
            if (!race.getRegistrationCloseAt().isBefore(race.getScheduledTime())) {
                throw new IllegalArgumentException("Registration close time must be before race scheduled time");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(race.getRegistrationOpenAt()) || now.isAfter(race.getRegistrationCloseAt())) {
                throw new IllegalArgumentException("Current time must be within registration period to open entry");
            }
        }

        race.setStatus(newStatus);
        return toResponse(raceRepository.save(race));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
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

    private RaceStatus parseRaceStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }
        String normalized = statusStr.trim().toLowerCase();
        switch (normalized) {
            case "registration_open":
            case "open_for_entry":
                return RaceStatus.OPEN_FOR_ENTRY;
            case "registration_closed":
            case "closed_for_entry":
                return RaceStatus.CLOSED_FOR_ENTRY;
            case "provisional_result":
            case "result_pending":
                return RaceStatus.RESULT_PENDING;
            case "official_result":
            case "official":
                return RaceStatus.OFFICIAL;
            default:
                for (RaceStatus status : RaceStatus.values()) {
                    if (status.name().equalsIgnoreCase(normalized)) {
                        return status;
                    }
                }
                throw new IllegalArgumentException("Race status is invalid");
        }
    }

    private RaceResponse toResponse(Race race) {
        Staff staff = race.getStaff();
        Referee referee = race.getReferee();
        return RaceResponse.builder()
                .raceId(race.getRaceId())
                .meetingId(race.getRaceMeeting().getMeetingId())
                .meetingName(race.getRaceMeeting().getMeetingName())
                .meetingDate(race.getRaceMeeting().getMeetingDate())
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
                .status(race.getStatus() != null ? race.getStatus().name() : "DRAFT")
                .createdAt(race.getCreatedAt())
                .updatedAt(race.getUpdatedAt())
                .build();
    }
}
