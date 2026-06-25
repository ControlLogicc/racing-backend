package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceInvitation;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RaceInvitationRepository extends JpaRepository<RaceInvitation, Long> {

    boolean existsByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatusIn(
            Long registrationId, Long jockeyId, List<RaceInvitationStatus> statuses);

    boolean existsByRaceRegistrationRegistrationIdAndInvitationStatus(
            Long registrationId, RaceInvitationStatus status);

    List<RaceInvitation> findByJockeyUserUserId(Long userId);

    List<RaceInvitation> findByJockeyUserUserIdAndInvitationStatus(
            Long userId, RaceInvitationStatus status);

    Optional<RaceInvitation> findByRaceRegistrationRegistrationIdAndInvitationStatus(
            Long registrationId, RaceInvitationStatus status);

    Optional<RaceInvitation> findByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatus(
            Long registrationId, Long jockeyId, RaceInvitationStatus status);
    boolean existsByRaceRegistrationRegistrationIdAndInvitationStatusIn(
            Long registrationId, List<RaceInvitationStatus> statuses);

    Optional<RaceInvitation> findTopByRaceRegistrationRegistrationIdOrderByCreatedAtDesc(Long registrationId);

    List<RaceInvitation> findByRaceRegistrationHorseOwnerUserId(Long userId);

    List<RaceInvitation> findByRaceRegistrationHorseOwnerUserIdAndInvitationStatus(
            Long userId, RaceInvitationStatus status);

    @Query("""
            select invitation from RaceInvitation invitation
            join fetch invitation.raceRegistration registration
            join fetch registration.race race
            left join fetch race.staff staff
            left join fetch staff.user
            join fetch registration.horse horse
            join fetch horse.owner
            join fetch invitation.jockey jockey
            join fetch jockey.user
            where race.raceId = :raceId
              and invitation.invitationStatus = :status
            order by invitation.sentAt asc
            """)
    List<RaceInvitation> findByRaceIdAndInvitationStatusWithDetails(
            @Param("raceId") Long raceId,
            @Param("status") RaceInvitationStatus status);

    @Query("""
            select invitation from RaceInvitation invitation
            join fetch invitation.raceRegistration registration
            join fetch registration.race race
            left join fetch race.staff staff
            left join fetch staff.user
            join fetch registration.horse horse
            join fetch horse.owner
            join fetch invitation.jockey jockey
            join fetch jockey.user
            where invitation.invitationId = :invitationId
            """)
    Optional<RaceInvitation> findByIdWithEntryDetails(@Param("invitationId") Long invitationId);
}
