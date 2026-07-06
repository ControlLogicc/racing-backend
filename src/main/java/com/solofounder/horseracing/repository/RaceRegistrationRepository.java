package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceRegistration;
import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RaceRegistrationRepository extends JpaRepository<RaceRegistration, Long> {
    List<RaceRegistration> findByRaceRaceId(Long raceId);
    List<RaceRegistration> findByRaceStaffStaffId(Long staffId);
    List<RaceRegistration> findBySubmittedByUserId(Long userId);
    boolean existsByRaceRaceIdAndHorseHorseId(Long raceId, Long horseId);
    boolean existsByRaceRaceIdAndSubmittedByUserId(Long raceId, Long ownerId);
    boolean existsByRaceRaceIdAndHorseHorseIdAndStatusIn(Long raceId, Long horseId, List<RaceRegistrationStatus> statuses);
    boolean existsByRaceRaceIdAndSubmittedByUserIdAndStatusIn(Long raceId, Long ownerId, List<RaceRegistrationStatus> statuses);
    long countByRaceRaceIdAndStatus(Long raceId, RaceRegistrationStatus status);

    @Query("""
            select registration from RaceRegistration registration
            join fetch registration.race race
            join fetch registration.horse horse
            join fetch registration.submittedBy owner
            where owner.userId = :ownerId
              and registration.status = :status
            order by registration.createdAt desc
            """)
    List<RaceRegistration> findBySubmittedByUserIdAndStatusWithDetails(
            @Param("ownerId") Long ownerId,
            @Param("status") RaceRegistrationStatus status);
}
