package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.race.CreateRaceResultRequest;
import com.solofounder.horseracing.dto.race.RaceResultResponse;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceEntry;
import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.PrizeStructureRepository;
import com.solofounder.horseracing.repository.RaceEntryRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import com.solofounder.horseracing.repository.RaceResultRepository;
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
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RaceResultService {

    private static final EnumSet<RaceStatus> RECORDABLE_RACE_STATUSES = EnumSet.of(
            RaceStatus.RUNNING,
            RaceStatus.RESULT_PENDING,
            RaceStatus.OFFICIAL
    );

    private static final EnumSet<RaceResultStatus> PRIZE_ELIGIBLE_STATUSES = EnumSet.of(
            RaceResultStatus.OFFICIAL,
            RaceResultStatus.AMENDED
    );

    private final RaceResultRepository raceResultRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PrizeStructureRepository prizeStructureRepository;

    public RaceResultResponse createResult(CreateRaceResultRequest request) {
        User currentUser = getCurrentUser();
        requireRecorderRole(currentUser);

        RaceEntry entry = raceEntryRepository.findByIdWithDetails(request.getEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race entry not found"));
        Race race = entry.getRace();

        if (!RECORDABLE_RACE_STATUSES.contains(race.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race status does not allow result recording");
        }
        if (currentUser.getRole() == Role.STAFF) {
            requireAssignedStaff(currentUser, race);
        }
        if (raceResultRepository.existsByEntryEntryId(entry.getEntryId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Race entry already has a result");
        }

        long entryCount = raceEntryRepository.countByRaceRaceId(race.getRaceId());
        if (request.getPosition() > entryCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position exceeds race entry count");
        }
        if (raceResultRepository.existsByRaceRaceIdAndPosition(race.getRaceId(), request.getPosition())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Race position already has a result");
        }

        RaceResultStatus resultStatus = parseResultStatus(request.getResultStatus());
        PrizeValues prizeValues = calculatePrizeValues(race.getRaceId(), request.getPosition(), resultStatus);

        RaceResult result = RaceResult.builder()
                .entry(entry)
                .race(race)
                .position(request.getPosition())
                .finishTime(request.getFinishTime())
                .resultStatus(resultStatus)
                .prizeAmount(prizeValues.prizeAmount())
                .scoreAwarded(prizeValues.scoreAwarded())
                .build();

        return toResponse(raceResultRepository.save(result));
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

    private void requireRecorderRole(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private void requireAssignedStaff(User user, Race race) {
        Staff staff = staffRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff profile not found"));
        if (race.getStaff() == null || !race.getStaff().getStaffId().equals(staff.getStaffId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
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
        if (!PRIZE_ELIGIBLE_STATUSES.contains(resultStatus)) {
            return new PrizeValues(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return prizeStructureRepository.findByRaceRaceIdAndPosition(raceId, position)
                .map(prize -> new PrizeValues(nullToZero(prize.getAmount()), nullToZero(prize.getScore())))
                .orElseGet(() -> new PrizeValues(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private record PrizeValues(BigDecimal prizeAmount, BigDecimal scoreAwarded) {
    }
}
