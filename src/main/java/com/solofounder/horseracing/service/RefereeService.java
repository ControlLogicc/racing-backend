package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.referee.CreateRefereeRequest;
import com.solofounder.horseracing.dto.referee.RefereeResponse;
import com.solofounder.horseracing.dto.referee.UpdateRefereeRequest;
import com.solofounder.horseracing.model.Referee;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.RefereeRepository;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefereeService {

    private final RefereeRepository refereeRepository;
    private final UserRepository userRepository;

    public List<RefereeResponse> getAllReferees() {
        return refereeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public RefereeResponse createReferee(CreateRefereeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.REFEREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User role must be REFEREE");
        }

        if (refereeRepository.existsByUserUserId(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Referee profile already exists for this user");
        }

        if (refereeRepository.existsByLicenseNo(request.getLicenseNo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "License number already exists");
        }

        Referee referee = Referee.builder()
                .user(user)
                .licenseNo(request.getLicenseNo())
                .status(RefereeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(refereeRepository.save(referee));
    }

    public RefereeResponse updateReferee(Long refereeId, UpdateRefereeRequest request) {
        Referee referee = refereeRepository.findById(refereeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referee not found"));

        if (refereeRepository.existsByLicenseNoAndRefereeIdNot(request.getLicenseNo(), refereeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "License number already exists");
        }

        RefereeStatus status;
        try {
            status = RefereeStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
        }

        referee.setLicenseNo(request.getLicenseNo());
        referee.setStatus(status);

        return toResponse(refereeRepository.save(referee));
    }

    private RefereeResponse toResponse(Referee referee) {
        User user = referee.getUser();
        return RefereeResponse.builder()
                .refereeId(referee.getRefereeId())
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .licenseNo(referee.getLicenseNo())
                .status(referee.getStatus() != null ? referee.getStatus().name() : null)
                .createdAt(referee.getCreatedAt())
                .build();
    }
}
