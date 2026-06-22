package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.CreateRaceResultRequest;
import com.solofounder.horseracing.dto.race.RaceResultResponse;
import com.solofounder.horseracing.service.RaceResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class RaceResultController {

    private final RaceResultService raceResultService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RaceResultResponse> createResult(@Valid @RequestBody CreateRaceResultRequest request) {
        return ResponseEntity.ok(raceResultService.createResult(request));
    }

    @GetMapping("/horse/{horseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RaceResultResponse>> getResultsByHorse(@PathVariable Long horseId) {
        return ResponseEntity.ok(raceResultService.getResultsByHorse(horseId));
    }

    @GetMapping("/{raceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RaceResultResponse>> getResultsByRace(@PathVariable Long raceId) {
        return ResponseEntity.ok(raceResultService.getResultsByRace(raceId));
    }
}
