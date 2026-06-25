package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.CreateRaceResultsRequest;
import com.solofounder.horseracing.dto.race.RaceResultResponse;
import com.solofounder.horseracing.service.RaceResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
public class RaceResultsController {

    private final RaceResultService raceResultService;

    @PostMapping("/{raceId}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'REFEREE')")
    public ResponseEntity<List<RaceResultResponse>> createResultsForRace(
            @PathVariable Long raceId,
            @Valid @RequestBody CreateRaceResultsRequest request) {
        return ResponseEntity.ok(raceResultService.createResultsForRace(raceId, request));
    }
}
