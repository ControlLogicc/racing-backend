package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.RaceRequest;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.dto.race.RecalculatePrizesResponse;
import com.solofounder.horseracing.service.RaceService;
import com.solofounder.horseracing.service.PrizeCalculationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/races")
@RequiredArgsConstructor
public class AdminRaceController {

    private final RaceService raceService;
    private final PrizeCalculationService prizeCalculationService;

    @GetMapping
    public ResponseEntity<List<RaceResponse>> getAllRaces() {
        return ResponseEntity.ok(raceService.getAllRaces());
    }

    @GetMapping("/open")
    public ResponseEntity<List<RaceResponse>> getOpenRacesForAdmin() {
        return ResponseEntity.ok(raceService.getOpenRaces());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RaceResponse> getRace(@PathVariable Long id) {
        return ResponseEntity.ok(raceService.getRace(id));
    }

    @PostMapping
    public ResponseEntity<RaceResponse> createRace(@Valid @RequestBody RaceRequest request) {
        return ResponseEntity.ok(raceService.createRace(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RaceResponse> updateRace(@PathVariable Long id,
            @Valid @RequestBody RaceRequest request) {
        return ResponseEntity.ok(raceService.updateRace(id, request));
    }

    @PutMapping("/{id}/assign-staff")
    public ResponseEntity<RaceResponse> assignStaff(@PathVariable Long id,
                                                    @RequestBody java.util.Map<String, Long> payload) {
        Long staffId = payload.get("staffId");
        if (staffId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Staff id is required");
        }
        return ResponseEntity.ok(raceService.assignStaff(id, staffId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRace(@PathVariable Long id) {
        raceService.deleteRace(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{raceId}/recalculate-prizes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecalculatePrizesResponse> recalculatePrizes(@PathVariable Long raceId) {
        return ResponseEntity.ok(prizeCalculationService.recalculatePrizes(raceId));
    }
}
