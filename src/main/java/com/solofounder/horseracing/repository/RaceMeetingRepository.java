package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RaceMeetingRepository extends JpaRepository<RaceMeeting, Long> {

    @Query(value = "SELECT COUNT(*) FROM dbo.race WHERE meeting_id = :meetingId", nativeQuery = true)
    long countRaces(@Param("meetingId") Long meetingId);
}
