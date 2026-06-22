package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RaceEntryRepository extends JpaRepository<RaceEntry, Long> {
}
