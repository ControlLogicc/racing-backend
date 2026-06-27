package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RefereeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefereeReportRepository extends JpaRepository<RefereeReport, Long> {

    @Query("""
            select report from RefereeReport report
            join fetch report.race race
            join fetch report.referee referee
            join fetch referee.user
            left join fetch race.referee raceReferee
            left join fetch raceReferee.user
            left join fetch race.staff staff
            left join fetch staff.user
            order by report.createdAt desc
            """)
    List<RefereeReport> findAllWithDetails();

    @Query("""
            select report from RefereeReport report
            join fetch report.race race
            join fetch report.referee referee
            join fetch referee.user
            left join fetch race.referee raceReferee
            left join fetch raceReferee.user
            left join fetch race.staff staff
            left join fetch staff.user
            where race.raceId = :raceId
            order by report.createdAt desc
            """)
    List<RefereeReport> findByRaceRaceIdWithDetails(@Param("raceId") Long raceId);

    @Query("""
            select report from RefereeReport report
            join fetch report.race race
            join fetch report.referee referee
            join fetch referee.user
            left join fetch race.referee raceReferee
            left join fetch raceReferee.user
            left join fetch race.staff staff
            left join fetch staff.user
            where report.reportId = :reportId
            """)
    Optional<RefereeReport> findByIdWithDetails(@Param("reportId") Long reportId);
}
