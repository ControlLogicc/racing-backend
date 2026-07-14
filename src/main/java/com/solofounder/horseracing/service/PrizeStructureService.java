package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.prizestructure.PrizeStructureRequest;
import com.solofounder.horseracing.dto.prizestructure.PrizeStructureResponse;
import com.solofounder.horseracing.model.PrizeStructure;
import com.solofounder.horseracing.model.Race;
import com.solofounder.horseracing.repository.PrizeStructureRepository;
import com.solofounder.horseracing.repository.RaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrizeStructureService {

    private final PrizeStructureRepository prizeStructureRepository;
    private final RaceRepository raceRepository;

    public List<PrizeStructureResponse> getAllPrizeStructures() {
        return prizeStructureRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PrizeStructureResponse> getPrizeStructuresByRace(Long raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("Race id is required");
        }
        return prizeStructureRepository.findByRaceRaceIdOrderByPositionAsc(raceId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PrizeStructureResponse getPrizeStructure(Long prizeId) {
        return toResponse(findPrizeStructure(prizeId));
    }

    public PrizeStructureResponse createPrizeStructure(PrizeStructureRequest request) {
        Race race = findRace(request.getRaceId());
        Short position = validatePosition(request.getPosition());
        if (prizeStructureRepository.existsByRaceRaceIdAndPosition(race.getRaceId(), position)) {
            throw new IllegalArgumentException("Prize structure position already exists for this race");
        }
        
        BigDecimal amount = validateAmount(request.getAmount(), position);
        BigDecimal score = validateScore(request.getScore(), position);

        PrizeStructure prizeStructure = PrizeStructure.builder()
                .race(race)
                .position(position)
                .amount(amount)
                .score(score)
                .build();
        validatePrizeOrder(race.getRaceId(), prizeStructure, null);
        return toResponse(prizeStructureRepository.save(prizeStructure));
    }

    public PrizeStructureResponse updatePrizeStructure(Long prizeId, PrizeStructureRequest request) {
        PrizeStructure prizeStructure = findPrizeStructure(prizeId);
        Race race = findRace(request.getRaceId());
        Short position = validatePosition(request.getPosition());
        if (prizeStructureRepository.existsByRaceRaceIdAndPositionAndPrizeIdNot(race.getRaceId(), position, prizeId)) {
            throw new IllegalArgumentException("Prize structure position already exists for this race");
        }
        
        BigDecimal amount = validateAmount(request.getAmount(), position);
        BigDecimal score = validateScore(request.getScore(), position);

        prizeStructure.setRace(race);
        prizeStructure.setPosition(position);
        prizeStructure.setAmount(amount);
        prizeStructure.setScore(score);
        validatePrizeOrder(race.getRaceId(), prizeStructure, prizeId);
        return toResponse(prizeStructureRepository.save(prizeStructure));
    }

    public void deletePrizeStructure(Long prizeId) {
        PrizeStructure prizeStructure = findPrizeStructure(prizeId);
        prizeStructureRepository.delete(prizeStructure);
    }

    private PrizeStructure findPrizeStructure(Long prizeId) {
        return prizeStructureRepository.findById(prizeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prize structure not found"));
    }

    private Race findRace(Long raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("Race id is required");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
    }

    private Short validatePosition(Short position) {
        if (position == null) {
            throw new IllegalArgumentException("Position is required");
        }
        if (position <= 0) {
            throw new IllegalArgumentException("Position must be greater than 0");
        }
        return position;
    }

    private BigDecimal validateAmount(BigDecimal amount, Short position) {
        if (position >= 4 && amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required for position " + position);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be greater than or equal to 0");
        }
        return amount;
    }

    private BigDecimal validateScore(BigDecimal score, Short position) {
        if ((position == 4 || position == 5 || position == 6) && score == null) {
            score = BigDecimal.ZERO;
        }
        if (score == null) {
            throw new IllegalArgumentException("Score is required for position " + position);
        }
        if (position <= 6) {
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Score for positions 1 to 6 must be greater than or equal to 0");
            }
        } else {
            if (score.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalArgumentException("Score for position 7 and below must be negative (less than 0)");
            }
        }
        return score;
    }

    private void validatePrizeOrder(Long raceId, PrizeStructure candidate, Long ignoredPrizeId) {
        List<PrizeStructure> prizeStructures = prizeStructureRepository.findByRaceRaceIdOrderByPositionAsc(raceId)
                .stream()
                .filter(prize -> ignoredPrizeId == null || !ignoredPrizeId.equals(prize.getPrizeId()))
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
        prizeStructures.add(candidate);
        prizeStructures.sort(java.util.Comparator.comparing(PrizeStructure::getPosition));

        for (int i = 1; i < prizeStructures.size(); i++) {
            PrizeStructure previous = prizeStructures.get(i - 1);
            PrizeStructure current = prizeStructures.get(i);
            if (current.getAmount().compareTo(previous.getAmount()) > 0) {
                throw new IllegalArgumentException("Prize amount for a lower position cannot be greater than a higher position");
            }
            if (current.getScore().compareTo(previous.getScore()) > 0) {
                throw new IllegalArgumentException("Prize score for a lower position cannot be greater than a higher position");
            }
        }
    }

    private PrizeStructureResponse toResponse(PrizeStructure prizeStructure) {
        Race race = prizeStructure.getRace();
        return PrizeStructureResponse.builder()
                .prizeId(prizeStructure.getPrizeId())
                .raceId(race.getRaceId())
                .raceName(race.getRaceName())
                .position(prizeStructure.getPosition())
                .amount(prizeStructure.getAmount())
                .score(prizeStructure.getScore())
                .createdAt(prizeStructure.getCreatedAt())
                .build();
    }
}
