package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceInvitation;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RaceInvitationRepository extends JpaRepository<RaceInvitation, Long> {
    List<RaceInvitation> findByRaceRegistrationSubmittedByUserId(Long ownerId);

    List<RaceInvitation> findByJockeyJockeyId(Long jockeyId);

    boolean existsByRaceRegistrationRegistrationIdAndJockeyJockeyIdAndStatusIn(
            Long registrationId,
            Long jockeyId,
            Collection<RaceInvitationStatus> statuses);

    boolean existsByRaceRegistrationRegistrationIdAndStatus(
            Long registrationId,
            RaceInvitationStatus status);
}
