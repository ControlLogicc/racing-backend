package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Horse;
import com.solofounder.horseracing.model.enums.HorseRegistrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HorseRepository extends JpaRepository<Horse, Long> {
    List<Horse> findByOwnerUserId(Long ownerId);

    Optional<Horse> findByHorseIdAndOwnerUserId(Long horseId, Long ownerId);

    @Query("""
            SELECT h FROM Horse h
            JOIN FETCH h.owner
            WHERE h.registrationType = :registrationType
              AND h.ratingVerified = false
              AND h.claimedScore IS NOT NULL
              AND h.claimedClass IS NOT NULL
              AND h.healthNote IS NOT NULL
              AND h.healthNote <> ''
            ORDER BY h.horseId DESC
            """)
    List<Horse> findPendingRatingHorses(@Param("registrationType") HorseRegistrationType registrationType);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_registration WHERE horse_id = :horseId", nativeQuery = true)
    long countRaceRegistrations(@Param("horseId") Long horseId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_entry WHERE horse_id = :horseId", nativeQuery = true)
    long countRaceEntries(@Param("horseId") Long horseId);
}
