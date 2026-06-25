package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Jockey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JockeyRepository extends JpaRepository<Jockey, Long> {
    Optional<Jockey> findByUserUserId(Long userId);

    boolean existsByUserUserId(Long userId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_invitation WHERE jockey_id = :jockeyId", nativeQuery = true)
    long countRaceInvitations(@Param("jockeyId") Long jockeyId);

    @Query(value = "SELECT COUNT(*) FROM dbo.race_entry WHERE jockey_id = :jockeyId", nativeQuery = true)
    long countRaceEntries(@Param("jockeyId") Long jockeyId);

    @Query(value = "SELECT COUNT(*) FROM dbo.jockey_race_registration WHERE jockey_id = :jockeyId", nativeQuery = true)
    long countRaceRegistrations(@Param("jockeyId") Long jockeyId);
}
