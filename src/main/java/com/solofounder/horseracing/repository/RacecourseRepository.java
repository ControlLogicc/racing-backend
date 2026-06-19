package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Racecourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RacecourseRepository extends JpaRepository<Racecourse, Long> {

    @Query(value = "SELECT COUNT(*) FROM dbo.race_meeting WHERE racecourse_id = :racecourseId", nativeQuery = true)
    long countRaceMeetings(@Param("racecourseId") Long racecourseId);
}
