package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.race.RecalculatePrizesResponse;
import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.PrizeStructure;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.HorseRepository;
import com.solofounder.horseracing.repository.PrizeStructureRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PrizeCalculationService {

    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    private final PrizeStructureRepository prizeStructureRepository;
    private final HorseRepository horseRepository;
    private final HorseService horseService;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    public RecalculatePrizesResponse recalculatePrizes(Long raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        requireRecalculatePermission(race);

        if (race.getStatus() != RaceStatus.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is not OFFICIAL");
        }

        List<RaceResult> results = raceResultRepository.findByRaceRaceId(raceId);
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race has no results");
        }

        int totalFinishers = (int) results.stream()
                .filter(this::isPrizeEligible)
                .count();

        BigDecimal totalPrizeAmount = BigDecimal.ZERO;
        BigDecimal totalScoreAwarded = BigDecimal.ZERO;
        Set<Horse> affectedHorses = new HashSet<>();

        for (RaceResult result : results) {
            if (isPrizeEligible(result)) {
                if (result.getResultStatus() == RaceResultStatus.PROVISIONAL) {
                    result.setResultStatus(RaceResultStatus.OFFICIAL);
                }
                applyPrizeAndScore(raceId, totalFinishers, result);
            } else {
                result.setPrizeAmount(BigDecimal.ZERO);
                result.setScoreAwarded(BigDecimal.ZERO);
            }

            if (result.getPrizeAmount() != null) {
                totalPrizeAmount = totalPrizeAmount.add(result.getPrizeAmount());
            }
            if (result.getScoreAwarded() != null) {
                totalScoreAwarded = totalScoreAwarded.add(result.getScoreAwarded());
            }
            if (result.getEntry() != null && result.getEntry().getHorse() != null) {
                affectedHorses.add(result.getEntry().getHorse());
            }
        }

        raceResultRepository.saveAll(results);
        affectedHorses.forEach(this::recalculateHorseStats);

        return RecalculatePrizesResponse.builder()
                .raceId(raceId)
                .raceName(race.getRaceName())
                .processedResults(results.size())
                .updatedHorseCount(affectedHorses.size())
                .totalPrizeAmount(totalPrizeAmount)
                .totalScoreAwarded(totalScoreAwarded)
                .message("Prize and score recalculation completed")
                .build();
    }

    private boolean isPrizeEligible(RaceResult result) {
        return result.getPosition() != null
                && result.getPosition() > 0
                && result.getResultStatus() != RaceResultStatus.DISQUALIFIED;
    }

    private void applyPrizeAndScore(Long raceId, int totalFinishers, RaceResult result) {
        Optional<PrizeStructure> prizeOpt = prizeStructureRepository.findByRaceRaceIdAndPosition(
                raceId,
                result.getPosition()
        );
        if (prizeOpt.isPresent()) {
            PrizeStructure prize = prizeOpt.get();
            result.setPrizeAmount(prize.getAmount());
            result.setScoreAwarded(prize.getScore());
            return;
        }

        result.setPrizeAmount(BigDecimal.ZERO);
        result.setScoreAwarded(resolveAutomaticScore(result.getPosition(), totalFinishers));
    }

    private void recalculateHorseStats(Horse horse) {
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

        long totalWins = raceResultRepository.countWinsByHorseIdExcludingStatus(
                horse.getHorseId(),
                RaceResultStatus.DISQUALIFIED
        );

        horse.setCurrentScore(currentScore);
        horse.setHorseClass(horseService.calculateHorseClass(currentScore));
        horse.setTotalWins((int) totalWins);
        horseRepository.save(horse);
    }

    private BigDecimal resolveAutomaticScore(int position, int totalFinishers) {
        if (position == 1) {
            return BigDecimal.valueOf(5);
        }
        if (position == 2) {
            return BigDecimal.valueOf(3);
        }
        if (position == 3) {
            return BigDecimal.ONE;
        }
        if (totalFinishers >= 1 && position == totalFinishers) {
            return BigDecimal.valueOf(-5);
        }
        if (totalFinishers >= 2 && position == totalFinishers - 1) {
            return BigDecimal.valueOf(-3);
        }
        if (totalFinishers >= 3 && position == totalFinishers - 2) {
            return BigDecimal.valueOf(-1);
        }
        return BigDecimal.ZERO;
    }

    private void requireRecalculatePermission(Race race) {
        User user = getCurrentUser();
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
