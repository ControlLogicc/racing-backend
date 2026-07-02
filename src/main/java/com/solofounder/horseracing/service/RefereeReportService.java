package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.report.CreateRefereeReportRequest;
import com.solofounder.horseracing.dto.report.RefereeReportResponse;
import com.solofounder.horseracing.dto.report.UpdateRefereeReportRequest;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.RefereeReport;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RefereeReportType;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceResultRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RefereeReportService {

    private static final Set<String> ALLOWED_DECISIONS = Set.of(
            "warning",
            "no_action",
            "disqualified",
            "dnf",
            "scratched"
    );

    private final RefereeReportRepository refereeReportRepository;
    private final RaceRepository raceRepository;
    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceResultRepository raceResultRepository;
    private final PrizeCalculationService prizeCalculationService;

    public RefereeReportResponse createReport(CreateRefereeReportRequest request) {
        User currentUser = getCurrentUser();
        requireRole(currentUser, Role.REFEREE);

        Referee referee = refereeRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referee profile not found"));
        Race race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));

        requireAssignedReferee(race, referee);
        RefereeReportType reportType = parseReportType(request.getReportType());
        RaceEntry entry = resolveEntry(request.getEntryId(), race);
        String description = firstNonBlank(request.getDescription(), request.getContent());
        if (description == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }
        String requestedDecision = firstNonBlank(request.getDecision(), request.getDecisions());
        String decisionAction = normalizeDecision(requestedDecision);
        requireEntryForDisciplinaryDecision(entry, decisionAction);
        String databaseDecision = toDatabaseDecision(decisionAction);

        RefereeReport report = RefereeReport.builder()
                .race(race)
                .referee(referee)
                .entry(entry)
                .reportType(reportType)
                .description(description)
                .penalty(firstNonBlank(request.getPenalty(), request.getViolations()))
                .decision(databaseDecision)
                .reportStatus(defaultReportStatus(request.getReportStatus()))
                .build();

        RefereeReport saved = refereeReportRepository.save(report);
        applyDecisionToStandings(entry, decisionAction);
        return toResponse(saved);
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
        String description = firstNonBlank(request.getDescription(), request.getContent());
        if (description == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }
        String requestedDecision = firstNonBlank(request.getDecision(), request.getDecisions());
        String decisionAction = normalizeDecision(requestedDecision);
        requireEntryForDisciplinaryDecision(report.getEntry(), decisionAction);
        String databaseDecision = toDatabaseDecision(decisionAction);
        report.setDescription(description);
        report.setPenalty(firstNonBlank(request.getPenalty(), request.getViolations()));
        report.setDecision(databaseDecision);
        if (request.getReportStatus() != null) {
            report.setReportStatus(defaultReportStatus(request.getReportStatus()));
        }
        applyDecisionToStandings(report.getEntry(), decisionAction);

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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String first, String second) {
        String normalized = blankToNull(first);
        return normalized != null ? normalized : blankToNull(second);
    }

    private String defaultReportStatus(String status) {
        String normalized = blankToNull(status);
        return normalized == null ? "submitted" : normalized.toLowerCase();
    }

    private String normalizeDecision(String requestedDecision) {
        String normalized = blankToNull(requestedDecision);
        if (normalized == null) {
            return "no_action";
        }
        normalized = normalized.toLowerCase();
        if (!ALLOWED_DECISIONS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Decision must be warning, no_action, disqualified, dnf, or scratched"
            );
        }
        return normalized;
    }

    private String toDatabaseDecision(String decisionAction) {
        return switch (decisionAction) {
            case "no_action" -> "no_issue";
            case "dnf", "scratched" -> "penalized";
            default -> decisionAction;
        };
    }

    private String toApiDecision(RefereeReport report) {
        if (report == null || report.getDecision() == null) {
            return null;
        }
        if ("no_issue".equalsIgnoreCase(report.getDecision())) {
            return "no_action";
        }
        if ("penalized".equalsIgnoreCase(report.getDecision()) && report.getEntry() != null) {
            String entryStatus = report.getEntry().getEntryStatus();
            if ("dnf".equalsIgnoreCase(entryStatus) || "scratched".equalsIgnoreCase(entryStatus)) {
                return entryStatus.toLowerCase();
            }
        }
        return report.getDecision().toLowerCase();
    }

    private void requireEntryForDisciplinaryDecision(RaceEntry entry, String decisionAction) {
        if (isDisciplinaryDecision(decisionAction) && entry == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Entry id is required for disciplinary decision"
            );
        }
    }

    private boolean isDisciplinaryDecision(String decision) {
        return "disqualified".equals(decision)
                || "dnf".equals(decision)
                || "scratched".equals(decision);
    }

    private RaceEntry resolveEntry(Long entryId, Race race) {
        if (entryId == null) {
            return null;
        }
        RaceEntry entry = raceEntryRepository.findByIdWithDetails(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));
        if (!entry.getRace().getRaceId().equals(race.getRaceId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race entry does not belong to this race");
        }
        return entry;
    }

    private void applyDecisionToStandings(RaceEntry entry, String decision) {
        if (!isDisciplinaryDecision(decision)) {
            return;
        }
        if (entry == null || entry.getRace() == null || entry.getRace().getRaceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid race entry data");
        }
        entry.setEntryStatus(decision.toLowerCase());
        raceEntryRepository.save(entry);

        List<RaceResult> loadedResults = raceResultRepository.findByRaceRaceId(entry.getRace().getRaceId());
        if (loadedResults == null || loadedResults.isEmpty()) {
            return;
        }
        List<RaceResult> allResults = new ArrayList<>(loadedResults);
        validateResultData(allResults);

        RaceResult affected = allResults.stream()
                .filter(result -> result.getEntry() != null
                        && entry.getEntryId().equals(result.getEntry().getEntryId()))
                .findFirst()
                .orElse(null);
        if (affected == null) {
            return;
        }
        affected.setResultStatus(RaceResultStatus.DISQUALIFIED);
        affected.setPrizeAmount(java.math.BigDecimal.ZERO);
        affected.setScoreAwarded(java.math.BigDecimal.ZERO);

        allResults.sort(java.util.Comparator.comparing(RaceResult::getPosition));
        short temporaryPosition = 1000;
        for (RaceResult result : allResults) {
            result.setPosition(temporaryPosition++);
        }
        raceResultRepository.saveAllAndFlush(allResults);

        List<RaceResult> ranked = allResults.stream()
                .filter(result -> result.getResultStatus() != RaceResultStatus.DISQUALIFIED)
                .sorted(java.util.Comparator.comparing(RaceResult::getPosition))
                .toList();
        short position = 1;
        for (RaceResult result : ranked) {
            result.setPosition(position++);
        }
        for (RaceResult result : allResults) {
            if (result.getResultStatus() == RaceResultStatus.DISQUALIFIED) {
                result.setPosition(position++);
            }
        }
        raceResultRepository.saveAll(allResults);
        prizeCalculationService.recalculateAfterStandingsChange(entry.getRace().getRaceId());
    }

    private void validateResultData(List<RaceResult> results) {
        Set<Short> positions = new HashSet<>();
        for (RaceResult result : results) {
            if (result == null || result.getResultId() == null || result.getEntry() == null
                    || result.getEntry().getEntryId() == null || result.getPosition() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid race result data");
            }
            if (!positions.add(result.getPosition())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate race result position");
            }
        }
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
        RaceEntry entry = report.getEntry();

        return RefereeReportResponse.builder()
                .reportId(report.getReportId())
                .raceId(race != null ? race.getRaceId() : null)
                .raceName(race != null ? race.getRaceName() : null)
                .entryId(entry != null ? entry.getEntryId() : null)
                .horseId(entry != null ? entry.getHorse().getHorseId() : null)
                .horseName(entry != null ? entry.getHorse().getHorseName() : null)
                .jockeyId(entry != null ? entry.getJockey().getJockeyId() : null)
                .jockeyName(entry != null ? entry.getJockey().getUser().getFullName() : null)
                .refereeId(referee != null ? referee.getRefereeId() : null)
                .refereeName(refereeUser != null ? refereeUser.getFullName() : null)
                .reportType(report.getReportType() != null ? report.getReportType().name() : null)
                .content(report.getDescription())
                .violations(report.getPenalty())
                .decisions(toApiDecision(report))
                .description(report.getDescription())
                .decision(toApiDecision(report))
                .penalty(report.getPenalty())
                .reportStatus(report.getReportStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
