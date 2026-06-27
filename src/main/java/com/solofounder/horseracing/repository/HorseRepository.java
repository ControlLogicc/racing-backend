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

    /** Horses that need Staff rating verification */
    List<Horse> findByRegistrationTypeAndRatingVerifiedFalse(HorseRegistrationType registrationType);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_registration WHERE horse_id = :horseId", nativeQuery = true)
    long countRaceRegistrations(@Param("horseId") Long horseId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_entry WHERE horse_id = :horseId", nativeQuery = true)
    long countRaceEntries(@Param("horseId") Long horseId);
}
