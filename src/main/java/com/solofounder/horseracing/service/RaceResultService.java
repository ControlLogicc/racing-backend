package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.race.CreateRaceResultRequest;
import com.solofounder.horseracing.dto.race.CreateRaceResultsRequest;
import com.solofounder.horseracing.dto.race.RaceResultResponse;
import com.solofounder.horseracing.dto.jockey.JockeyStatsResponse;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.PrizeStructureRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RaceResultRepository;
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
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceResultService {

    private static final EnumSet<RaceStatus> RECORDABLE_RACE_STATUSES = EnumSet.of(
            RaceStatus.RUNNING,
            RaceStatus.RESULT_PENDING,
            RaceStatus.OFFICIAL
    );

    private final RaceResultRepository raceResultRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;
    private final JockeyRepository jockeyRepository;
    private final HorseService horseService;
    private final StaffRepository staffRepository;
    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;
    private final PrizeStructureRepository prizeStructureRepository;

    public RaceResultResponse createResult(CreateRaceResultRequest request) {
        User currentUser = getCurrentUser();
        requireRecorderRole(currentUser);

        RaceEntry entry = raceEntryRepository.findByIdWithDetails(request.getEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));
        Race race = entry.getRace();

        if (!isResultEligibleEntry(entry)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only READY race entries can have a result");
        }

        if (!RECORDABLE_RACE_STATUSES.contains(race.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race status does not allow result recording");
        }
        if (currentUser.getRole() == Role.STAFF) {
            requireAssignedStaff(currentUser, race);
        }
        if (currentUser.getRole() == Role.REFEREE) {
            requireAssignedReferee(currentUser, race);
        }
        if (raceResultRepository.existsByEntryEntryId(entry.getEntryId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Race entry already has a result");
        }

        RaceResultStatus resultStatus = parseResultStatus(request.getResultStatus());
        validateOptionalPosition(race.getRaceId(), request.getPosition());
        validateFinishTimeRequired(resultStatus, request.getFinishTime());
        validateUniqueFinishTime(race.getRaceId(), request.getFinishTime(), resultStatus, null);

        RaceResult result = RaceResult.builder()
                .entry(entry)
                .race(race)
                .position(nextTemporaryPosition(race.getRaceId()))
                .finishTime(request.getFinishTime())
                .resultStatus(resultStatus)
                .prizeAmount(BigDecimal.ZERO)
                .scoreAwarded(BigDecimal.ZERO)
                .build();

        RaceResult saved = raceResultRepository.save(result);
        recalculateRacePositionsAndAwards(race.getRaceId());
        return findResultResponse(saved.getResultId());
    }

    public List<RaceResultResponse> createResultsForRace(Long raceId, CreateRaceResultsRequest request) {
        User currentUser = getCurrentUser();
        requireRecorderRole(currentUser);

        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        if (race.getStatus() != RaceStatus.RESULT_PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race must be in RESULT_PENDING status");
        }
        requireAssignedRecorder(currentUser, race);

        List<CreateRaceResultRequest> items = request.getResults();
        Set<Long> entryIds = new HashSet<>();
        Set<LocalTime> finishTimes = new HashSet<>();
        long entryCount = raceEntryRepository.countByRaceRaceId(raceId);

        for (CreateRaceResultRequest item : items) {
            if (!entryIds.add(item.getEntryId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate entry in results");
            }
            if (item.getPosition() != null && item.getPosition() > entryCount) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position exceeds race entry count");
            }

            RaceEntry entry = raceEntryRepository.findByIdWithDetails(item.getEntryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));
            if (entry.getRace() == null || !entry.getRace().getRaceId().equals(raceId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry does not belong to this race");
            }
            if (!isResultEligibleEntry(entry)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only READY race entries can have a result");
            }
            if (raceResultRepository.existsByEntryEntryId(entry.getEntryId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Race entry already has a result");
            }
            RaceResultStatus resultStatus = parseResultStatus(item.getResultStatus());
            validateFinishTimeRequired(resultStatus, item.getFinishTime());
            if (isRankableStatus(resultStatus) && item.getFinishTime() != null && !finishTimes.add(item.getFinishTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate finish time in results");
            }
            validateUniqueFinishTime(raceId, item.getFinishTime(), resultStatus, null);
        }

        List<Long> createdResultIds = new ArrayList<>();
        short nextPosition = nextTemporaryPosition(raceId);
        for (CreateRaceResultRequest item : items) {
            RaceEntry entry = raceEntryRepository.findByIdWithDetails(item.getEntryId()).orElseThrow();
            RaceResultStatus resultStatus = parseResultStatus(item.getResultStatus());
            RaceResult result = RaceResult.builder()
                    .entry(entry)
                    .race(race)
                    .position(nextPosition++)
                    .finishTime(item.getFinishTime())
                    .resultStatus(resultStatus)
                    .prizeAmount(BigDecimal.ZERO)
                    .scoreAwarded(BigDecimal.ZERO)
                    .build();
            RaceResult saved = raceResultRepository.save(result);
            createdResultIds.add(saved.getResultId());
        }
        recalculateRacePositionsAndAwards(raceId);

        return raceResultRepository.findByRaceRaceIdWithDetailsOrderByPositionAsc(raceId).stream()
                .filter(result -> createdResultIds.contains(result.getResultId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RaceResultResponse> getResultsByRace(Long raceId) {
        if (!raceRepository.existsById(raceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found");
        }
        return raceResultRepository.findByRaceRaceIdWithDetailsOrderByPositionAsc(raceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RaceResultResponse> getResultsByHorse(Long horseId) {
        if (!horseRepository.existsById(horseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found");
        }
        return raceResultRepository.findByHorseIdWithDetails(horseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RaceResultResponse> getResultsByJockey(Long jockeyId) {
        if (!jockeyRepository.existsById(jockeyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey not found");
        }
        return raceResultRepository.findByJockeyIdWithDetails(jockeyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JockeyStatsResponse getJockeyStats(Long jockeyId) {
        Jockey jockey = jockeyRepository.findById(jockeyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jockey not found"));
        List<RaceResult> results = raceResultRepository.findByJockeyIdWithDetails(jockeyId);

        long totalRaces = results.size();
        long totalWins = results.stream()
                .filter(this::isPrizeEligibleResult)
                .filter(result -> result.getPosition() != null && result.getPosition() == 1)
                .count();
        long top3Finishes = results.stream()
                .filter(this::isPrizeEligibleResult)
                .filter(result -> result.getPosition() != null && result.getPosition() <= 3)
                .count();
        long disqualifiedCount = results.stream()
                .filter(result -> result.getResultStatus() == RaceResultStatus.DISQUALIFIED)
                .count();
        BigDecimal totalPrizeAmount = results.stream()
                .map(RaceResult::getPrizeAmount)
                .map(this::nullToZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalScoreAwarded = results.stream()
                .map(RaceResult::getScoreAwarded)
                .map(this::nullToZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePosition = averagePosition(results);
        BigDecimal winRate = totalRaces == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalWins)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalRaces), 2, RoundingMode.HALF_UP);

        User jockeyUser = jockey.getUser();
        return JockeyStatsResponse.builder()
                .jockeyId(jockey.getJockeyId())
                .jockeyName(jockeyUser != null ? jockeyUser.getFullName() : null)
                .totalRaces(totalRaces)
                .totalWins(totalWins)
                .top3Finishes(top3Finishes)
                .disqualifiedCount(disqualifiedCount)
                .totalPrizeAmount(totalPrizeAmount)
                .totalScoreAwarded(totalScoreAwarded)
                .averagePosition(averagePosition)
                .winRate(winRate)
                .build();
    }

    private void requireRecorderRole(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF && user.getRole() != Role.REFEREE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private void requireAssignedRecorder(User user, Race race) {
        if (user.getRole() == Role.STAFF) {
            requireAssignedStaff(user, race);
        } else if (user.getRole() == Role.REFEREE) {
            requireAssignedReferee(user, race);
        }
    }

    private void requireAssignedStaff(User user, Race race) {
        Staff staff = staffRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
        if (race.getStaff() == null || !race.getStaff().getStaffId().equals(staff.getStaffId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private void requireAssignedReferee(User user, Race race) {
        Referee referee = refereeRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Referee profile not found"));
        if (race.getReferee() == null || !race.getReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private boolean isResultEligibleEntry(RaceEntry entry) {
        String status = entry.getEntryStatus();
        return "READY".equalsIgnoreCase(status) || "PASSED".equalsIgnoreCase(status);
    }

    private RaceResultStatus parseResultStatus(String status) {
        if (status == null || status.isBlank()) {
            return RaceResultStatus.PROVISIONAL;
        }
        try {
            return RaceResultStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid result status");
        }
    }

    private PrizeValues calculatePrizeValues(Long raceId, Short position, RaceResultStatus resultStatus) {
        if (resultStatus == RaceResultStatus.DISQUALIFIED || resultStatus == RaceResultStatus.PROVISIONAL) {
            return new PrizeValues(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return prizeStructureRepository.findByRaceRaceIdAndPosition(raceId, position)
                .map(prize -> new PrizeValues(nullToZero(prize.getAmount()), nullToZero(prize.getScore())))
                .orElseGet(() -> new PrizeValues(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateOptionalPosition(Long raceId, Short position) {
        if (position == null) {
            return;
        }
        long entryCount = raceEntryRepository.countByRaceRaceId(raceId);
        if (position > entryCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position exceeds race entry count");
        }
    }

    private void validateFinishTimeRequired(RaceResultStatus status, LocalTime finishTime) {
        if (isRankableStatus(status) && finishTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finish time is required");
        }
    }

    private void validateUniqueFinishTime(Long raceId, LocalTime finishTime, RaceResultStatus status,
            Long excludedResultId) {
        if (!isRankableStatus(status) || finishTime == null) {
            return;
        }
        boolean finishTimeTaken = raceResultRepository.findByRaceRaceId(raceId).stream()
                .anyMatch(existing -> (excludedResultId == null || !excludedResultId.equals(existing.getResultId()))
                        && isRankableResult(existing)
                        && finishTime.equals(existing.getFinishTime()));
        if (finishTimeTaken) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finish time already has a result");
        }
    }

    private boolean isRankableStatus(RaceResultStatus status) {
        return status != RaceResultStatus.DISQUALIFIED;
    }

    private boolean isRankableResult(RaceResult result) {
        return result.getFinishTime() != null && isRankableStatus(result.getResultStatus());
    }

    private boolean isPrizeEligibleResult(RaceResult result) {
        return result.getResultStatus() != RaceResultStatus.DISQUALIFIED;
    }

    private BigDecimal averagePosition(List<RaceResult> results) {
        List<Short> positions = results.stream()
                .filter(this::isPrizeEligibleResult)
                .map(RaceResult::getPosition)
                .filter(position -> position != null && position > 0)
                .toList();
        if (positions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = positions.stream()
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(positions.size()), 2, RoundingMode.HALF_UP);
    }

    private short nextTemporaryPosition(Long raceId) {
        long resultCount = raceResultRepository.findByRaceRaceId(raceId).size();
        return (short) (resultCount + 1);
    }

    private RaceResultResponse findResultResponse(Long resultId) {
        RaceResult result = raceResultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        return toResponse(result);
    }

    private void recalculateRacePositionsAndAwards(Long raceId) {
        List<RaceResult> results = raceResultRepository.findByRaceRaceId(raceId);
        List<RaceResult> rankedResults = results.stream()
                .filter(this::isRankableResult)
                .sorted(Comparator.comparing(RaceResult::getFinishTime)
                        .thenComparing(result -> result.getResultId() == null ? Long.MAX_VALUE : result.getResultId()))
                .toList();
        Set<Long> rankedResultIds = rankedResults.stream()
                .map(RaceResult::getResultId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Horse> affectedHorses = new HashSet<>();

        short position = 1;
        for (RaceResult result : rankedResults) {
            result.setPosition(position++);
            PrizeValues prizeValues = calculatePrizeValues(raceId, result.getPosition(), result.getResultStatus());
            result.setPrizeAmount(prizeValues.prizeAmount());
            result.setScoreAwarded(prizeValues.scoreAwarded());
            addAffectedHorse(affectedHorses, result);
        }

        for (RaceResult result : results) {
            if (rankedResultIds.contains(result.getResultId())) {
                continue;
            }
            result.setPosition(position++);
            result.setPrizeAmount(BigDecimal.ZERO);
            result.setScoreAwarded(BigDecimal.ZERO);
            addAffectedHorse(affectedHorses, result);
        }

        raceResultRepository.saveAll(results);
        affectedHorses.forEach(this::recalculateHorseScore);
    }

    private void addAffectedHorse(Set<Horse> affectedHorses, RaceResult result) {
        if (result.getEntry() != null && result.getEntry().getHorse() != null) {
            affectedHorses.add(result.getEntry().getHorse());
        }
    }

    private void recalculateHorseScore(Horse horse) {
        if (horse == null || horse.getHorseId() == null) {
            return;
        }
        BigDecimal startingScore = horse.getClaimedScore();
        if (startingScore == null) {
            startingScore = BigDecimal.ZERO;
        }
        if (horse.getRegistrationType() == com.solofounder.horseracing.model.enums.HorseRegistrationType.PREVIOUSLY_REGISTERED && !horse.isRatingVerified()) {
            startingScore = BigDecimal.ZERO;
        }

        BigDecimal currentScore = raceResultRepository.sumScoreByHorseIdExcludingStatus(
                horse.getHorseId(),
                RaceResultStatus.DISQUALIFIED
        );
        if (currentScore == null) {
            currentScore = BigDecimal.ZERO;
        }
        currentScore = startingScore.add(currentScore);

        horse.setCurrentScore(currentScore);
        horse.setHorseClass(horseService.calculateHorseClass(currentScore));
        horse.setTotalWins((int) raceResultRepository.countWinsByHorseIdExcludingStatus(
                horse.getHorseId(),
                RaceResultStatus.DISQUALIFIED
        ));
        horseRepository.save(horse);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private RaceResultResponse toResponse(RaceResult result) {
        RaceEntry entry = result.getEntry();
        Race race = result.getRace();
        Horse horse = entry != null ? entry.getHorse() : null;
        Jockey jockey = entry != null ? entry.getJockey() : null;
        User jockeyUser = jockey != null ? jockey.getUser() : null;

        return RaceResultResponse.builder()
                .resultId(result.getResultId())
                .entryId(entry != null ? entry.getEntryId() : null)
                .raceId(race != null ? race.getRaceId() : null)
                .raceName(race != null ? race.getRaceName() : null)
                .scheduledTime(race != null ? race.getScheduledTime() : null)
                .raceStatus(race != null && race.getStatus() != null ? race.getStatus().name() : null)
                .horseId(horse != null ? horse.getHorseId() : null)
                .horseName(horse != null ? horse.getHorseName() : null)
                .jockeyId(jockey != null ? jockey.getJockeyId() : null)
                .jockeyName(jockeyUser != null ? jockeyUser.getFullName() : null)
                .position(result.getPosition())
                .finishTime(result.getFinishTime())
                .resultStatus(result.getResultStatus() != null ? result.getResultStatus().name() : null)
                .prizeAmount(result.getPrizeAmount())
                .scoreAwarded(result.getScoreAwarded())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    public RaceResultResponse updateResult(Long resultId, Integer position, String finishTime) {
        RaceResult result = raceResultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        User currentUser = getCurrentUser();
        requireRecorderRole(currentUser);
        requireAssignedRecorder(currentUser, result.getRace());
        if (position != null) {
            validateOptionalPosition(result.getRace().getRaceId(), position.shortValue());
        }
        LocalTime nextFinishTime = finishTime != null && !finishTime.isBlank()
                ? parseFinishTime(finishTime)
                : result.getFinishTime();
        validateFinishTimeRequired(result.getResultStatus(), nextFinishTime);
        validateUniqueFinishTime(result.getRace().getRaceId(), nextFinishTime,
                result.getResultStatus(), result.getResultId());
        result.setFinishTime(nextFinishTime);
        RaceResult saved = raceResultRepository.save(result);
        recalculateRacePositionsAndAwards(saved.getRace().getRaceId());
        return findResultResponse(saved.getResultId());
    }

    private LocalTime parseFinishTime(String finishTime) {
        try {
            return LocalTime.parse(finishTime);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid finish time format");
        }
    }

    public void deleteResult(Long resultId) {
        RaceResult result = raceResultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        requireRecorderRole(getCurrentUser());
        raceResultRepository.delete(result);
    }

    private record PrizeValues(BigDecimal prizeAmount, BigDecimal scoreAwarded) {
    }
}
