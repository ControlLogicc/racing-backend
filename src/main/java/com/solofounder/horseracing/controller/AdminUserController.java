package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.user.CreateInternalUserRequest;
import com.solofounder.horseracing.dto.user.UpdateUserRequest;
import com.solofounder.horseracing.dto.user.UpdateUserStatusRequest;
import com.solofounder.horseracing.dto.user.UserResponse;
import com.solofounder.horseracing.service.AuthService;
import com.solofounder.horseracing.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;
    private final UserManagementService userManagementService;

    @PostMapping
    public ResponseEntity<UserResponse> createInternalUser(@RequestBody CreateInternalUserRequest request) {
        UserResponse response = userManagementService.mapToUserResponse(authService.adminCreateUser(request));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userManagementService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(userManagementService.updateUserStatus(id, request));
    }
}
