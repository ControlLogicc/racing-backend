package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceInvitation;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RaceInvitationRepository extends JpaRepository<RaceInvitation, Long> {

    boolean existsByRegistrationRegistrationIdAndJockeyJockeyIdAndInvitationStatusIn(
            Long registrationId, Long jockeyId, List<RaceInvitationStatus> statuses);

    boolean existsByRegistrationRegistrationIdAndInvitationStatus(
            Long registrationId, RaceInvitationStatus status);

    List<RaceInvitation> findByJockeyUserUserId(Long userId);

    List<RaceInvitation> findByJockeyUserUserIdAndInvitationStatus(
            Long userId, RaceInvitationStatus status);

    Optional<RaceInvitation> findByRegistrationRegistrationIdAndInvitationStatus(
            Long registrationId, RaceInvitationStatus status);
}
