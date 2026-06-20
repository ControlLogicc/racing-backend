package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.staff.CreateStaffRequest;
import com.solofounder.horseracing.dto.staff.StaffResponse;
import com.solofounder.horseracing.dto.staff.UpdateStaffRequest;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private static final Set<String> VALID_STATUSES = Set.of("active", "inactive");

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;

    public StaffResponse getCurrentStaffProfile() {
        User user = getCurrentUser();
        requireRole(user, Role.STAFF);
        Staff staff = staffRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found"));
        return toResponse(staff);
    }

    public StaffResponse createStaff(CreateStaffRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.STAFF) {
            throw new IllegalArgumentException("User role must be STAFF");
        }
        if (staffRepository.existsByUserUserId(user.getUserId())) {
            throw new IllegalStateException("Staff profile already exists for this user");
        }

        String staffCode = normalizeRequired(request.getStaffCode(), "Staff code is required");
        if (staffRepository.existsByStaffCode(staffCode)) {
            throw new IllegalStateException("Staff code already exists");
        }

        Staff staff = Staff.builder()
                .user(user)
                .staffCode(staffCode)
                .department(normalizeRequired(request.getDepartment(), "Department is required"))
                .status(normalizeStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(staffRepository.save(staff));
    }

    public StaffResponse updateStaff(Long staffId, UpdateStaffRequest request) {
        Staff staff = findStaff(staffId);
        String staffCode = normalizeRequired(request.getStaffCode(), "Staff code is required");
        staffRepository.findByStaffCode(staffCode)
                .filter(existing -> !existing.getStaffId().equals(staff.getStaffId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Staff code already exists");
                });

        staff.setStaffCode(staffCode);
        staff.setDepartment(normalizeRequired(request.getDepartment(), "Department is required"));
        staff.setStatus(normalizeStatus(request.getStatus()));
        return toResponse(staffRepository.save(staff));
    }

    private Staff findStaff(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private void requireRole(User user, Role role) {
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "active";
        }
        normalized = normalized.toLowerCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Staff status must be active or inactive");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private StaffResponse toResponse(Staff staff) {
        User user = staff.getUser();
        return StaffResponse.builder()
                .staffId(staff.getStaffId())
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .staffCode(staff.getStaffCode())
                .department(staff.getDepartment())
                .status(staff.getStatus())
                .createdAt(staff.getCreatedAt())
                .build();
    }
}
