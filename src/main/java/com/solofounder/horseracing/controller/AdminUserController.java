package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.user.CreateInternalUserRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<UserResponse> createInternalUser(@Valid @RequestBody CreateInternalUserRequest request) {
        User createdUser = authService.adminCreateUser(request);
        UserResponse response = UserResponse.builder()
                .userId(createdUser.getUserId())
                .fullName(createdUser.getFullName())
                .email(createdUser.getEmail())
                .phone(createdUser.getPhone())
                .role(createdUser.getRole())
                .status(createdUser.getStatus())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = authService.getAllUsers().stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .build())
                .toList();
        return ResponseEntity.ok(responses);
    }
}
