package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.admin.CreateInternalAccountRequest;
import com.solofounder.horseracing.dto.admin.InternalAccountResponse;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalAccountService {

    private static final Set<Role> PROFILE_ROLES = Set.of(Role.STAFF, Role.JOCKEY, Role.REFEREE);
    private static final Set<String> STAFF_STATUSES = Set.of("active", "inactive");
    private static final Set<String> JOCKEY_STATUSES = Set.of("available", "unavailable", "suspended");

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final JockeyRepository jockeyRepository;
    private final RefereeRepository refereeRepository;
    private final PasswordEncoder passwordEncoder;

    public InternalAccountResponse createInternalAccount(CreateInternalAccountRequest request) {
        Role role = request.getRole();
        if (!PROFILE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Only STAFF, JOCKEY and REFEREE can be created with profile");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }

        User user = userRepository.save(User.builder()
                .fullName(trimRequired(request.getFullName(), "Full name is required"))
                .email(trimRequired(request.getEmail(), "Email is required"))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(trimToNull(request.getPhone()))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());

        Long profileId = switch (role) {
            case STAFF -> createStaffProfile(user, request);
            case JOCKEY -> createJockeyProfile(user, request);
            case REFEREE -> createRefereeProfile(user, request);
            default -> throw new IllegalArgumentException("Only STAFF, JOCKEY and REFEREE can be created with profile");
        };

        return InternalAccountResponse.builder()
                .userId(user.getUserId())
                .profileId(profileId)
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .build();
    }

    private Long createStaffProfile(User user, CreateInternalAccountRequest request) {
        String staffCode = trimRequired(request.getStaffCode(), "Staff code is required");
        if (staffRepository.existsByStaffCode(staffCode)) {
            throw new IllegalStateException("Staff code already exists");
        }
        if (staffRepository.existsByUserUserId(user.getUserId())) {
            throw new IllegalStateException("Staff profile already exists for this user");
        }

        Staff staff = staffRepository.save(Staff.builder()
                .user(user)
                .staffCode(staffCode)
                .department(trimRequired(firstNonBlank(request.getDepartment(), request.getPosition()), "Department is required"))
                .status(normalizeStaffStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .build());
        return staff.getStaffId();
    }

    private Long createJockeyProfile(User user, CreateInternalAccountRequest request) {
        if (jockeyRepository.existsByUserUserId(user.getUserId())) {
            throw new IllegalStateException("Jockey profile already exists for this user");
        }

        Jockey jockey = jockeyRepository.save(Jockey.builder()
                .user(user)
                .weight(validatePositiveWeight(request.getWeight()))
                .experienceYears(validateExperienceYears(request.getExperienceYears()))
                .status(normalizeJockeyStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .build());
        return jockey.getJockeyId();
    }

    private Long createRefereeProfile(User user, CreateInternalAccountRequest request) {
        if (refereeRepository.existsByUserUserId(user.getUserId())) {
            throw new IllegalStateException("Referee profile already exists for this user");
        }

        String licenseNo = trimRequired(firstNonBlank(request.getLicenseNo(), request.getLicenseNumber()), "License number is required");
        if (refereeRepository.existsByLicenseNo(licenseNo)) {
            throw new IllegalStateException("License number already exists");
        }

        Referee referee = refereeRepository.save(Referee.builder()
                .user(user)
                .licenseNo(licenseNo)
                .status(normalizeRefereeStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .build());
        return referee.getRefereeId();
    }

    private BigDecimal validatePositiveWeight(BigDecimal weight) {
        if (weight == null) {
            throw new IllegalArgumentException("Weight is required");
        }
        if (weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
        return weight;
    }

    private Short validateExperienceYears(Short experienceYears) {
        if (experienceYears != null && experienceYears < 0) {
            throw new IllegalArgumentException("Experience years must be greater than or equal to 0");
        }
        return experienceYears;
    }

    private String normalizeStaffStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "active";
        }
        normalized = normalized.toLowerCase();
        if (!STAFF_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Staff status must be active or inactive");
        }
        return normalized;
    }

    private String normalizeJockeyStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "available";
        }
        normalized = normalized.toLowerCase();
        if (!JOCKEY_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Jockey status must be available, unavailable, or suspended");
        }
        return normalized;
    }

    private RefereeStatus normalizeRefereeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return RefereeStatus.ACTIVE;
        }
        try {
            return RefereeStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Referee status must be active, inactive, or suspended");
        }
    }

    private String trimRequired(String value, String message) {
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

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = trimToNull(first);
        return normalizedFirst != null ? normalizedFirst : trimToNull(second);
    }
}
