package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.race.RecalculatePrizesResponse;
import com.solofounder.horseracing.model.*;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

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
        // 1. Retrieve Race
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
        requireRecalculatePermission(race);

        // 2. Check Race status: must be OFFICIAL
        if (race.getStatus() != RaceStatus.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race is not OFFICIAL");
        }

        // 3. Retrieve all results for the race
        List<RaceResult> results = raceResultRepository.findByRaceRaceId(raceId);
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Race has no results");
        }

        // Determine total number of finishers (eligible positions only)
        int totalFinishers = (int) results.stream()
                .filter(r -> r.getPosition() != null && r.getPosition() > 0
                        && (r.getResultStatus() == RaceResultStatus.OFFICIAL || r.getResultStatus() == RaceResultStatus.AMENDED))
                .count();

        BigDecimal totalPrizeAmount = BigDecimal.ZERO;
        BigDecimal totalScoreAwarded = BigDecimal.ZERO;
        Set<Horse> affectedHorses = new HashSet<>();

        // 4. Update result prizes & scores
        for (RaceResult result : results) {
            boolean eligible = result.getPosition() != null && result.getPosition() > 0
                    && result.getResultStatus() != RaceResultStatus.DISQUALIFIED;
            
                    && (result.getResultStatus() == RaceResultStatus.OFFICIAL || result.getResultStatus() == RaceResultStatus.AMENDED);

            if (eligible) {
                if (result.getResultStatus() == RaceResultStatus.PROVISIONAL) {
                    result.setResultStatus(RaceResultStatus.OFFICIAL);
                }
                Optional<PrizeStructure> prizeOpt = prizeStructureRepository.findByRaceRaceIdAndPosition(raceId, result.getPosition());
                if (prizeOpt.isPresent()) {
                    PrizeStructure ps = prizeOpt.get();
                    result.setPrizeAmount(ps.getAmount());
                    result.setScoreAwarded(ps.getScore());
                } else {
                    // No manual prize structure — apply automatic position-based scoring rule:
                    // Top 3: +5 / +3 / +1
                    // Bottom 3: -1 / -3 / -5  (counting from last place up)
                    result.setPrizeAmount(BigDecimal.ZERO);
                    result.setScoreAwarded(resolveAutomaticScore(result.getPosition(), totalFinishers));
                }
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

        // Save updated results
        raceResultRepository.saveAll(results);

        // 5. Recalculate scores, classes, wins for affected horses
        for (Horse horse : affectedHorses) {
            BigDecimal currentScore = raceResultRepository.sumScoreByHorseIdExcludingStatus(
                    horse.getHorseId(),
                    RaceResultStatus.DISQUALIFIED
            );
            if (currentScore == null) {
                currentScore = BigDecimal.ZERO;
            }
            
            // Re-classify class using the existing helper method
            Short newClass = horseService.calculateHorseClass(currentScore);

            long totalWins = raceResultRepository.countWinsByHorseIdExcludingStatus(
                    horse.getHorseId(),
                    RaceResultStatus.DISQUALIFIED
            );

            horse.setCurrentScore(currentScore);
            horse.setHorseClass(newClass);
            horse.setTotalWins((int) totalWins);

            horseRepository.save(horse);
        }

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
    /**
     * Automatic position-based scoring rule (applied when no PrizeStructure is defined):
     *   - 1st place : +5
     *   - 2nd place : +3
     *   - 3rd place : +1
     *   - Last place     (N)   : -5
     *   - 2nd last place (N-1) : -3
     *   - 3rd last place (N-2) : -1
     *   - All other positions  :  0
     *
     * @param position      the finish position (1-indexed)
     * @param totalFinishers total number of eligible finishers in this race
     */
    private BigDecimal resolveAutomaticScore(int position, int totalFinishers) {
        // Top 3
        if (position == 1) return BigDecimal.valueOf(5);
        if (position == 2) return BigDecimal.valueOf(3);
        if (position == 3) return BigDecimal.valueOf(1);

        // Bottom 3 (only meaningful when race has enough finishers)
        if (totalFinishers >= 1 && position == totalFinishers)     return BigDecimal.valueOf(-5);
        if (totalFinishers >= 2 && position == totalFinishers - 1) return BigDecimal.valueOf(-3);
        if (totalFinishers >= 3 && position == totalFinishers - 2) return BigDecimal.valueOf(-1);

        return BigDecimal.ZERO;
    }
}
