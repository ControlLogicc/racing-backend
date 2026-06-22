package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaceEntryRepository extends JpaRepository<RaceEntry, Long> {
    boolean existsByRegistrationRegistrationId(Long registrationId);
    boolean existsByRaceRaceIdAndJockeyJockeyId(Long raceId, Long jockeyId);
    boolean existsByRaceRaceIdAndHorseHorseId(Long raceId, Long horseId);
    boolean existsByRaceRaceIdAndGateNumber(Long raceId, Short gateNumber);
    List<RaceEntry> findByRaceRaceId(Long raceId);
}
