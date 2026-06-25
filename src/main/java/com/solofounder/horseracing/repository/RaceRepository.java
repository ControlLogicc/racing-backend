package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.solofounder.horseracing.model.enums.RaceStatus;

public interface RaceRepository extends JpaRepository<Race, Long> {
    List<Race> findByStatus(RaceStatus status);

    List<Race> findByStaffStaffId(Long staffId);

    @Query("SELECT r FROM Race r " +
            "JOIN FETCH r.raceMeeting m " +
            "JOIN FETCH m.racecourse " +
            "JOIN FETCH r.raceCondition " +
            "LEFT JOIN FETCH r.staff s " +
            "LEFT JOIN FETCH s.user " +
            "LEFT JOIN FETCH r.referee rf " +
            "LEFT JOIN FETCH rf.user " +
            "WHERE rf.refereeId = :refereeId")
    List<Race> findByRefereeRefereeIdWithDetails(@Param("refereeId") Long refereeId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_registration WHERE race_id = :raceId", nativeQuery = true)
    long countRaceRegistrations(@Param("raceId") Long raceId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_entry WHERE race_id = :raceId", nativeQuery = true)
    long countRaceEntries(@Param("raceId") Long raceId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_result WHERE race_id = :raceId", nativeQuery = true)
    long countRaceResults(@Param("raceId") Long raceId);

    @Query(value = "SELECT COUNT(*) FROM dbo.jockey_race_registration WHERE race_id = :raceId", nativeQuery = true)
    long countJockeyRaceRegistrations(@Param("raceId") Long raceId);
}
