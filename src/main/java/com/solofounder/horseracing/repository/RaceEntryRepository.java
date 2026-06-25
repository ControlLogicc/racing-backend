package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

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

    boolean existsByRegistrationRegistrationId(Long registrationId);

    Optional<RaceEntry> findByRegistrationRegistrationId(Long registrationId);

    boolean existsByInvitationInvitationId(Long invitationId);

    boolean existsByRaceRaceIdAndJockeyJockeyId(Long raceId, Long jockeyId);

    boolean existsByRaceRaceIdAndHorseHorseId(Long raceId, Long horseId);

    boolean existsByRaceRaceIdAndGateNumber(Long raceId, Short gateNumber);

    @Query("""
            SELECT COALESCE(MAX(e.gateNumber), 0)
            FROM RaceEntry e
            WHERE e.race.raceId = :raceId
            """)
    int findMaxGateNumberByRaceId(@Param("raceId") Long raceId);

    List<RaceEntry> findByRaceRaceId(Long raceId);

    @Query("SELECT e FROM RaceEntry e " +
            "JOIN FETCH e.race r " +
            "LEFT JOIN FETCH r.staff rs " +
            "LEFT JOIN FETCH rs.user " +
            "LEFT JOIN FETCH r.referee rr " +
            "LEFT JOIN FETCH rr.user " +
            "JOIN FETCH e.horse " +
            "JOIN FETCH e.jockey j " +
            "JOIN FETCH j.user " +
            "LEFT JOIN FETCH e.confirmedByStaff cs " +
            "LEFT JOIN FETCH cs.user " +
            "WHERE r.raceId = :raceId")
    List<RaceEntry> findByRaceRaceIdWithDetails(@Param("raceId") Long raceId);
}
