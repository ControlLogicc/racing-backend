package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RaceRegistrationRepository extends JpaRepository<RaceRegistration, Long> {
    List<RaceRegistration> findByRaceRaceId(Long raceId);
    boolean existsByRaceRaceIdAndHorseHorseId(Long raceId, Long horseId);
    long countByRaceRaceIdAndStatus(Long raceId, RaceRegistrationStatus status);
}
