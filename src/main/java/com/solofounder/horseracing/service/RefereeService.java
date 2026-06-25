package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.dto.referee.CreateRefereeRequest;
import com.solofounder.horseracing.dto.referee.RefereeResponse;
import com.solofounder.horseracing.dto.referee.UpdateRefereeRequest;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.Role;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefereeService {

    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;
    private final RaceRepository raceRepository;

    public List<RefereeResponse> getAllReferees() {
        return refereeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RefereeResponse getCurrentReferee() {
        return toResponse(getCurrentRefereeProfile());
    }

    @Transactional(readOnly = true)
    public List<RaceResponse> getCurrentRefereeRaces() {
        Referee referee = getCurrentRefereeProfile();
        return raceRepository.findByRefereeRefereeId(referee.getRefereeId()).stream()
                .map(this::toRaceResponse)
                .toList();
    }

    public RefereeResponse createReferee(CreateRefereeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.REFEREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User role must be REFEREE");
        }

        if (refereeRepository.existsByUserUserId(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Referee profile already exists for this user");
        }

        if (refereeRepository.existsByLicenseNo(request.getLicenseNo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "License number already exists");
        }

        Referee referee = Referee.builder()
                .user(user)
                .licenseNo(request.getLicenseNo())
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(refereeRepository.save(referee));
    }

    public RefereeResponse updateReferee(Long refereeId, UpdateRefereeRequest request) {
        Referee referee = refereeRepository.findById(refereeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referee not found"));

        if (refereeRepository.existsByLicenseNoAndRefereeIdNot(request.getLicenseNo(), refereeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "License number already exists");
        }

        RefereeStatus status;
        try {
            status = RefereeStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
        }

        referee.setLicenseNo(request.getLicenseNo());
        referee.setStatus(status);

        return toResponse(refereeRepository.save(referee));
    }

    private RefereeResponse toResponse(Referee referee) {
        User user = referee.getUser();
        return RefereeResponse.builder()
                .refereeId(referee.getRefereeId())
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .licenseNo(referee.getLicenseNo())
                .status(referee.getStatus() != null ? referee.getStatus().name() : null)
                .createdAt(referee.getCreatedAt())
                .build();
    }

    private Referee getCurrentRefereeProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (user.getRole() != Role.REFEREE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return refereeRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referee profile not found"));
    }

    private RaceResponse toRaceResponse(Race race) {
        var meeting = race.getRaceMeeting();
        var condition = race.getRaceCondition();
        var racecourse = meeting != null ? meeting.getRacecourse() : null;
        var staff = race.getStaff();
        var referee = race.getReferee();
        return RaceResponse.builder()
                .raceId(race.getRaceId())
                .meetingId(meeting != null ? meeting.getMeetingId() : null)
                .meetingName(meeting != null ? meeting.getMeetingName() : null)
                .meetingDate(meeting != null ? meeting.getMeetingDate() : null)
                .racecourseName(racecourse != null ? racecourse.getRacecourseName() : null)
                .conditionId(condition != null ? condition.getConditionId() : null)
                .conditionName(condition != null ? condition.getConditionName() : null)
                .distanceMeters(condition != null ? condition.getDistance() : null)
                .trackType(condition != null ? condition.getTrackType() : null)
                .classRequirement(condition != null ? condition.getClassRequirement() : null)
                .staffId(staff != null ? staff.getStaffId() : null)
                .staffName(staff != null && staff.getUser() != null ? staff.getUser().getFullName() : null)
                .refereeId(referee != null ? referee.getRefereeId() : null)
                .refereeName(referee != null && referee.getUser() != null ? referee.getUser().getFullName() : null)
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
}
