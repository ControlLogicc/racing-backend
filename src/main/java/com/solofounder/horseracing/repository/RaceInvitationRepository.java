package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceInvitation;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<RaceInvitation> findByRaceRegistrationHorseOwnerUserId(Long userId);

    List<RaceInvitation> findByRaceRegistrationHorseOwnerUserIdAndInvitationStatus(
            Long userId, RaceInvitationStatus status);
}
