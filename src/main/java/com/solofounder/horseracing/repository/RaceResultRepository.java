package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceResult;
import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.enums.RaceResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    List<RaceResult> findByRaceRaceId(Long raceId);

    boolean existsByEntryEntryId(Long entryId);

    boolean existsByRaceRaceIdAndPosition(Long raceId, Short position);

    @Query("SELECT r FROM RaceResult r " +
           "JOIN FETCH r.entry e " +
           "JOIN FETCH r.race race " +
           "JOIN FETCH e.horse " +
           "JOIN FETCH e.jockey j " +
           "JOIN FETCH j.user " +
           "WHERE race.raceId = :raceId " +
           "ORDER BY r.position ASC")
    List<RaceResult> findByRaceRaceIdWithDetailsOrderByPositionAsc(@Param("raceId") Long raceId);

    @Query("SELECT r FROM RaceResult r " +
           "JOIN FETCH r.entry e " +
           "JOIN FETCH r.race race " +
           "JOIN FETCH e.horse h " +
           "JOIN FETCH e.jockey j " +
           "JOIN FETCH j.user " +
           "WHERE h.horseId = :horseId " +
           "ORDER BY race.scheduledTime DESC, r.createdAt DESC")
    List<RaceResult> findByHorseIdWithDetails(@Param("horseId") Long horseId);

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

    @Query("SELECT COALESCE(SUM(r.scoreAwarded), 0) FROM RaceResult r " +
           "WHERE r.entry.horse.horseId = :horseId " +
           "AND r.resultStatus <> :excludedStatus")
    BigDecimal sumScoreByHorseIdExcludingStatus(
            @Param("horseId") Long horseId,
            @Param("excludedStatus") RaceResultStatus excludedStatus);

    @Query("SELECT COUNT(r) FROM RaceResult r " +
           "WHERE r.entry.horse.horseId = :horseId " +
           "AND r.resultStatus <> :excludedStatus " +
           "AND r.position = 1")
    long countWinsByHorseIdExcludingStatus(
            @Param("horseId") Long horseId,
            @Param("excludedStatus") RaceResultStatus excludedStatus);
}
