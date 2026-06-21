package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.jockey.JockeyResponse;
import com.solofounder.horseracing.dto.jockey.UpdateJockeyWeightRequest;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.service.JockeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jockeys")
@RequiredArgsConstructor
public class JockeyController {

    private final JockeyService jockeyService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JockeyResponse>> getJockeys() {
        return ResponseEntity.ok(jockeyService.getAllJockeys());
    }

    @PutMapping("/{id}/weight")
    @PreAuthorize("hasRole('JOCKEY')")
    public ResponseEntity<JockeyResponse> updateOwnWeight(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateJockeyWeightRequest request) {
        return ResponseEntity.ok(jockeyService.updateOwnWeight(id, request));
    }

    @GetMapping("/available-races")
    @PreAuthorize("hasRole('JOCKEY')")
    public ResponseEntity<List<RaceResponse>> getAvailableRaces() {
        return ResponseEntity.ok(jockeyService.getAvailableRacesForCurrentJockey());
    }
}
