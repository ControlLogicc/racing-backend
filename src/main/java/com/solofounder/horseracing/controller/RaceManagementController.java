package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.service.RaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/race-management/races")
@RequiredArgsConstructor
public class RaceManagementController {

    private final RaceService raceService;

    @PatchMapping("/{raceId}/status")
    public ResponseEntity<RaceResponse> updateRaceStatus(
            @PathVariable Long raceId,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        return ResponseEntity.ok(raceService.updateRaceStatus(raceId, status));
    }
}
