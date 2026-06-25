package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.jockey.CreateJockeyRaceRegistrationRequest;
import com.solofounder.horseracing.dto.jockey.JockeyRaceRegistrationResponse;
import com.solofounder.horseracing.service.JockeyRaceRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jockey/race-registrations")
@RequiredArgsConstructor
public class JockeyRaceRegistrationController {

    private final JockeyRaceRegistrationService jockeyRaceRegistrationService;

    @PostMapping
    @PreAuthorize("hasRole('JOCKEY')")
    public ResponseEntity<JockeyRaceRegistrationResponse> registerForRace(
            @Valid @RequestBody CreateJockeyRaceRegistrationRequest request) {
        return ResponseEntity.ok(jockeyRaceRegistrationService.registerCurrentJockey(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('JOCKEY')")
    public ResponseEntity<List<JockeyRaceRegistrationResponse>> getMyRaceRegistrations() {
        return ResponseEntity.ok(jockeyRaceRegistrationService.getCurrentJockeyRegistrations());
    }
}
