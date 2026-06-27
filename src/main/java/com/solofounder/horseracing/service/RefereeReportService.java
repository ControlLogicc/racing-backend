package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.report.CreateRefereeReportRequest;
import com.solofounder.horseracing.dto.report.RefereeReportResponse;
import com.solofounder.horseracing.dto.report.UpdateRefereeReportRequest;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.RefereeReport;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RefereeReportType;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RefereeReportRepository;
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
@Transactional
public class RefereeReportService {

    private final RefereeReportRepository refereeReportRepository;
    private final RaceRepository raceRepository;
    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;

    public RefereeReportResponse createReport(CreateRefereeReportRequest request) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.REFEREE);

        Referee referee = refereeRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referee profile not found"));
        Race race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));

        requireAssignedReferee(race, referee);
        RefereeReportType reportType = parseReportType(request.getReportType());

        RefereeReport report = RefereeReport.builder()
                .race(race)
                .referee(referee)
                .reportType(reportType)
                .description(request.getContent())
                .penalty(blankToNull(request.getViolations()))
                .decision(resolveDecision(reportType, request.getDecisions()))
                .reportStatus("submitted")
                .build();

        return toResponse(refereeReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<RefereeReportResponse> getAllReports() {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.ADMIN);

        return refereeReportRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RefereeReportResponse> getReportsByRace(Long raceId) {
        User currentUser = getCurrentUser();
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));

        if (!canViewRaceReports(currentUser, race)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        return refereeReportRepository.findByRaceRaceIdWithDetails(raceId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RefereeReportResponse updateReport(Long reportId, UpdateRefereeReportRequest request) {
        User currentUser = getCurrentUser();
        RefereeReport report = refereeReportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        if (!canUpdateReport(currentUser, report)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        RefereeReportType reportType = parseReportType(request.getReportType());
        report.setReportType(reportType);
        report.setDescription(request.getContent());
        report.setPenalty(blankToNull(request.getViolations()));
        report.setDecision(resolveDecision(reportType, request.getDecisions()));

        return toResponse(refereeReportRepository.save(report));
    }

    private boolean canViewRaceReports(User user, Race race) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() == Role.REFEREE) {
            return race.getReferee() != null
                    && race.getReferee().getUser() != null
                    && race.getReferee().getUser().getUserId().equals(user.getUserId());
        }
        if (user.getRole() == Role.STAFF) {
            return race.getStaff() != null
                    && race.getStaff().getUser() != null
                    && race.getStaff().getUser().getUserId().equals(user.getUserId());
        }
        return false;
    }

    private boolean canUpdateReport(User user, RefereeReport report) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getRole() == Role.REFEREE
                && report.getReferee() != null
                && report.getReferee().getUser() != null
                && report.getReferee().getUser().getUserId().equals(user.getUserId());
    }

    private void requireAssignedReferee(Race race, Referee referee) {
        if (race.getReferee() == null || !race.getReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private RefereeReportType parseReportType(String reportType) {
        try {
            return RefereeReportType.valueOf(reportType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report type");
        }
    }

    private String resolveDecision(RefereeReportType reportType, String decisions) {
        if (decisions != null && !decisions.isBlank()) {
            String normalized = decisions.trim().toLowerCase().replace(' ', '_');
            switch (normalized) {
                case "no_issue":
                case "warning":
                case "penalized":
                case "disqualified":
                case "result_confirmed":
                    return normalized;
                default:
                    break;
            }
        }
        switch (reportType) {
            case VIOLATION:
                return "warning";
            case DECISION:
                return "result_confirmed";
            case PRE_RACE:
            default:
                return "no_issue";
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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

    private RefereeReportResponse toResponse(RefereeReport report) {
        Race race = report.getRace();
        Referee referee = report.getReferee();
        User refereeUser = referee != null ? referee.getUser() : null;

        return RefereeReportResponse.builder()
                .reportId(report.getReportId())
                .raceId(race != null ? race.getRaceId() : null)
                .raceName(race != null ? race.getRaceName() : null)
                .refereeId(referee != null ? referee.getRefereeId() : null)
                .refereeName(refereeUser != null ? refereeUser.getFullName() : null)
                .reportType(report.getReportType() != null ? report.getReportType().name() : null)
                .content(report.getDescription())
                .violations(report.getPenalty())
                .decisions(report.getDecision())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
