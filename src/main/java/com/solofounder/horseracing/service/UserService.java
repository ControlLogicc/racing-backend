package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.user.BanUserRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public UserResponse banUser(Long userId, BanUserRequest request) {
        User admin = getCurrentAdmin();
        if (admin.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot ban own account");
        }

        User target = findRequiredUser(userId);
        target.setAccountStatus("banned");
        target.setBannedReason(request.getReason().trim());
        target.setBannedAt(LocalDateTime.now());
        target.setBannedBy(admin.getUserId());
        return toResponse(userRepository.save(target));
    }

    @Transactional
    public UserResponse unbanUser(Long userId) {
        getCurrentAdmin();
        User target = findRequiredUser(userId);
        target.setAccountStatus("active");
        target.setBannedReason(null);
        target.setBannedAt(null);
        target.setBannedBy(null);
        return toResponse(userRepository.save(target));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUserResponses() {
        getCurrentAdmin();
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .accountStatus(normalizeAccountStatus(user.getAccountStatus()))
                .bannedReason(user.getBannedReason())
                .bannedAt(user.getBannedAt())
                .bannedBy(user.getBannedBy())
                .build();
    }

    private User findRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return user;
    }

    private String normalizeAccountStatus(String accountStatus) {
        return accountStatus == null || accountStatus.isBlank()
                ? "active"
                : accountStatus.toLowerCase();
    }
}
