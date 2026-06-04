package com.solofounder.horseracing.controllers;

import com.solofounder.horseracing.repository.UserRepository;
import com.solofounder.horseracing.util.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final UserRepository userRepository;

    public HealthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Backend is running")
                .data("OK")
                .build();
    }

    @GetMapping("/db-test")
    public ApiResponse<Long> databaseTest() {
        long totalUsers = userRepository.count();

        return ApiResponse.<Long>builder()
                .success(true)
                .message("Database connected successfully")
                .data(totalUsers)
                .build();
    }
}