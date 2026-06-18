package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaceRepository extends JpaRepository<Race, Long> {
    List<Race> findByStatus(String status);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_registration WHERE race_id = :raceId", nativeQuery = true)
    long countRaceRegistrations(@Param("raceId") Long raceId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_entry WHERE race_id = :raceId", nativeQuery = true)
    long countRaceEntries(@Param("raceId") Long raceId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_result WHERE race_id = :raceId", nativeQuery = true)
    long countRaceResults(@Param("raceId") Long raceId);
}
