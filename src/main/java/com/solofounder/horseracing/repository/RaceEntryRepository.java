package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RaceEntryRepository extends JpaRepository<RaceEntry, Long> {

    long countByRaceRaceId(Long raceId);

    @Query("SELECT e FROM RaceEntry e " +
            "JOIN FETCH e.race r " +
            "LEFT JOIN FETCH r.staff rs " +
            "LEFT JOIN FETCH rs.user " +
            "JOIN FETCH e.horse " +
            "JOIN FETCH e.jockey j " +
            "JOIN FETCH j.user " +
            "WHERE e.entryId = :entryId")
    Optional<RaceEntry> findByIdWithDetails(@Param("entryId") Long entryId);
}
