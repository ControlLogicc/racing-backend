package com.solofounder.horseracing.repository;

import com.solofounder.horseracing.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUserUserId(Long userId);

    boolean existsByUserUserId(Long userId);

    boolean existsByStaffCode(String staffCode);

    Optional<Staff> findByStaffCode(String staffCode);
}
