package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.user.CreateInternalUserRequest;
import com.solofounder.horseracing.dto.user.BanUserRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.model.User;
import com.solofounder.horseracing.service.AuthService;
import com.solofounder.horseracing.service.UserService;
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
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createInternalUser(@Valid @RequestBody CreateInternalUserRequest request) {
        User createdUser = authService.adminCreateUser(request);
        return ResponseEntity.ok(userService.toResponse(createdUser));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUserResponses());
    }

    @PutMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> banUser(
            @PathVariable Long userId,
            @Valid @RequestBody BanUserRequest request) {
        return ResponseEntity.ok(userService.banUser(userId, request));
    }

    @PutMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> unbanUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.unbanUser(userId));
    }
}
