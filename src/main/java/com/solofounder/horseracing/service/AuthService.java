package com.solofounder.horseracing.service;

import com.solofounder.horseracing.config.JwtService;
import com.solofounder.horseracing.dto.auth.AuthResponse;
import com.solofounder.horseracing.dto.auth.LoginRequest;
import com.solofounder.horseracing.dto.auth.RegisterRequest;
import com.solofounder.horseracing.dto.user.CreateInternalUserRequest;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.Jockey;
import com.solofounder.horseracing.model.Staff;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.UserStatus;
import com.solofounder.horseracing.repository.JockeyRepository;
import com.solofounder.horseracing.repository.UserRepository;
import com.solofounder.horseracing.repository.StaffRepository;
import com.solofounder.horseracing.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JockeyRepository jockeyRepository;
    private final StaffRepository staffRepository;
    private final RefereeRepository refereeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private static final Set<Role> PUBLIC_ROLES = Set.of(Role.OWNER, Role.JOCKEY, Role.SPECTATOR);
    private static final Set<Role> ADMIN_ROLES = Set.of(Role.ADMIN, Role.STAFF, Role.REFEREE);

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }

        if (!PUBLIC_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException("Role not allowed for public registration: " + request.getRole());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == Role.JOCKEY) {
            Jockey jockey = Jockey.builder()
                    .user(savedUser)
                    .status("available")
                    .build();
            jockeyRepository.save(jockey);
        }

        String jwtToken = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(savedUser.getUserId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User status is not ACTIVE. Current status: " + user.getStatus());
        }

        // Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    public User adminCreateUser(CreateInternalUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == Role.STAFF) {
            String staffCode = request.getStaffCode();
            if (staffCode == null || staffCode.trim().isEmpty()) {
                staffCode = "STF-" + savedUser.getUserId();
            } else {
                staffCode = staffCode.trim();
            }
            if (staffRepository.existsByStaffCode(staffCode)) {
                throw new IllegalStateException("Staff code already exists");
            }
            String department = request.getDepartment();
            if (department == null || department.trim().isEmpty()) {
                department = "Operations";
            } else {
                department = department.trim();
            }
            Staff staff = Staff.builder()
                    .user(savedUser)
                    .staffCode(staffCode)
                    .department(department)
                    .status("active")
                    .createdAt(LocalDateTime.now())
                    .build();
            staffRepository.save(staff);
        } else if (savedUser.getRole() == Role.REFEREE) {
            String licenseNo = request.getLicenseNo();
            if (licenseNo == null || licenseNo.trim().isEmpty()) {
                licenseNo = "LIC-" + savedUser.getUserId();
            } else {
                licenseNo = licenseNo.trim();
            }
            if (refereeRepository.existsByLicenseNo(licenseNo)) {
                throw new IllegalStateException("License number already exists");
            }
            Referee referee = Referee.builder()
                    .user(savedUser)
                    .licenseNo(licenseNo)
                    .status(RefereeStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            refereeRepository.save(referee);
        } else if (savedUser.getRole() == Role.JOCKEY) {
            Jockey jockey = Jockey.builder()
                    .user(savedUser)
                    .weight(request.getWeight())
                    .experienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : (short) 0)
                    .status("available")
                    .build();
            jockeyRepository.save(jockey);
        }

        return savedUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
