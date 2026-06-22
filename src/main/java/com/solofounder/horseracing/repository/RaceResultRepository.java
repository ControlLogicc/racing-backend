package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    List<RaceResult> findByRaceRaceId(Long raceId);

    List<RaceResult> findByEntryHorseHorseIdAndRaceStatusAndResultStatusIn(
            Long horseId, RaceStatus raceStatus, List<RaceResultStatus> resultStatuses);

    @Query("SELECT COALESCE(SUM(r.scoreAwarded), 0) FROM RaceResult r " +
           "WHERE r.entry.horse.horseId = :horseId " +
           "AND r.race.status = :raceStatus " +
           "AND r.resultStatus IN :statuses")
    BigDecimal sumOfficialScoreByHorseId(
            @Param("horseId") Long horseId,
            @Param("raceStatus") RaceStatus raceStatus,
            @Param("statuses") List<RaceResultStatus> statuses);

    @Query("SELECT COUNT(r) FROM RaceResult r " +
           "WHERE r.entry.horse.horseId = :horseId " +
           "AND r.race.status = :raceStatus " +
           "AND r.resultStatus IN :statuses " +
           "AND r.position = 1")
    long countOfficialWinsByHorseId(
            @Param("horseId") Long horseId,
            @Param("raceStatus") RaceStatus raceStatus,
            @Param("statuses") List<RaceResultStatus> statuses);
}
