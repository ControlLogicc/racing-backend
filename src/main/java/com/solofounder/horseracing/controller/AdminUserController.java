package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.user.CreateInternalUserRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
