package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.PrizeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeStructureRepository extends JpaRepository<PrizeStructure, Long> {
    boolean existsByRaceRaceIdAndPosition(Long raceId, Short position);

    boolean existsByRaceRaceIdAndPositionAndPrizeIdNot(Long raceId, Short position, Long prizeId);
}
