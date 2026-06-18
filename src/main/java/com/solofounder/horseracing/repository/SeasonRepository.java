package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {

    @Query(value = "SELECT COUNT(*) FROM dbo.race_meeting WHERE season_id = :seasonId", nativeQuery = true)
    long countRaceMeetings(@Param("seasonId") Long seasonId);
}
