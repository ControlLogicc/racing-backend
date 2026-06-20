package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Referee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefereeRepository extends JpaRepository<Referee, Long> {
}
