package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Referee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefereeRepository extends JpaRepository<Referee, Long> {
    Optional<Referee> findByUserUserId(Long userId);
    boolean existsByUserUserId(Long userId);
    boolean existsByLicenseNo(String licenseNo);
    boolean existsByLicenseNoAndRefereeIdNot(String licenseNo, Long refereeId);
}
