package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.jockey.JockeyResponse;
import com.solofounder.horseracing.dto.jockey.UpdateJockeyRequest;
import com.solofounder.horseracing.service.JockeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jockey/profile")
@RequiredArgsConstructor
public class JockeyProfileController {

    private final JockeyService jockeyService;

    @GetMapping
    public ResponseEntity<JockeyResponse> getProfile() {
        return ResponseEntity.ok(jockeyService.getCurrentJockeyProfile());
    }

    @PutMapping
    public ResponseEntity<JockeyResponse> updateProfile(@RequestBody UpdateJockeyRequest request) {
        return ResponseEntity.ok(jockeyService.updateCurrentJockeyProfile(request));
    }
}
