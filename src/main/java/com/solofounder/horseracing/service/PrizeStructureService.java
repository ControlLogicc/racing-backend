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

    public PrizeStructureResponse getPrizeStructure(Long prizeId) {
        return toResponse(findPrizeStructure(prizeId));
    }

    public PrizeStructureResponse createPrizeStructure(PrizeStructureRequest request) {
        Race race = findRace(request.getRaceId());
        Short position = validatePosition(request.getPosition());
        if (prizeStructureRepository.existsByRaceRaceIdAndPosition(race.getRaceId(), position)) {
            throw new IllegalArgumentException("Prize structure position already exists for this race");
        }
        PrizeStructure prizeStructure = PrizeStructure.builder()
                .race(race)
                .position(position)
                .amount(validateMoney(request.getAmount(), "Amount is required"))
                .score(validateMoney(request.getScore(), "Score is required"))
                .build();
        return toResponse(prizeStructureRepository.save(prizeStructure));
    }

    public PrizeStructureResponse updatePrizeStructure(Long prizeId, PrizeStructureRequest request) {
        PrizeStructure prizeStructure = findPrizeStructure(prizeId);
        Race race = findRace(request.getRaceId());
        Short position = validatePosition(request.getPosition());
        if (prizeStructureRepository.existsByRaceRaceIdAndPositionAndPrizeIdNot(race.getRaceId(), position, prizeId)) {
            throw new IllegalArgumentException("Prize structure position already exists for this race");
        }
        prizeStructure.setRace(race);
        prizeStructure.setPosition(position);
        prizeStructure.setAmount(validateMoney(request.getAmount(), "Amount is required"));
        prizeStructure.setScore(validateMoney(request.getScore(), "Score is required"));
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

    private BigDecimal validateMoney(BigDecimal value, String requiredMessage) {
        if (value == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount and score must be greater than or equal to 0");
        }
        return value;
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
