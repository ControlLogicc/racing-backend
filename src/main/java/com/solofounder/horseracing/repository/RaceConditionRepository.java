package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RaceConditionRepository extends JpaRepository<RaceCondition, Long> {

    @Query(value = "SELECT COUNT(*) FROM dbo.race WHERE condition_id = :conditionId", nativeQuery = true)
    long countRaces(@Param("conditionId") Long conditionId);
}
