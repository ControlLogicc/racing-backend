package com.solofounder.horseracing.service;

import com.solofounder.horseracing.dto.user.UpdateUserRequest;
import com.solofounder.horseracing.dto.user.UpdateUserStatusRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return mapToUserResponse(findUserOrThrow(id));
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);

        userRepository.findByEmail(request.getEmail())
                .filter(existingUser -> !existingUser.getUserId().equals(id))
                .ifPresent(existingUser -> {
                    throw new IllegalStateException("Email already exists");
                });

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());

        return mapToUserResponse(userRepository.save(user));
    }

    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = findUserOrThrow(id);
        user.setStatus(request.getStatus());
        return mapToUserResponse(userRepository.save(user));
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
