package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.JockeyRaceRegistration;
import com.solofounder.horseracing.model.enums.JockeyRaceRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JockeyRaceRegistrationRepository extends JpaRepository<JockeyRaceRegistration, Long> {

    boolean existsByRaceRaceIdAndJockeyJockeyIdAndStatus(
            Long raceId,
            Long jockeyId,
            JockeyRaceRegistrationStatus status);

    List<JockeyRaceRegistration> findByJockeyUserUserIdOrderByRegisteredAtDesc(Long userId);

    @Query("""
            select registration from JockeyRaceRegistration registration
            join fetch registration.jockey jockey
            join fetch jockey.user
            where registration.race.raceId = :raceId
              and registration.status = :status
            order by registration.registeredAt asc
            """)
    List<JockeyRaceRegistration> findByRaceIdAndStatusWithJockey(
            @Param("raceId") Long raceId,
            @Param("status") JockeyRaceRegistrationStatus status);
}
