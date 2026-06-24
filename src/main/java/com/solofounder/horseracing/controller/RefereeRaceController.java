package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.entry.RaceEntryResponse;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.service.RefereeRaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referee")
@RequiredArgsConstructor
public class RefereeRaceController {

    private final RefereeRaceService refereeRaceService;

    @GetMapping("/races")
    @PreAuthorize("hasRole('REFEREE')")
    public ResponseEntity<List<RaceResponse>> getAssignedRaces() {
        return ResponseEntity.ok(refereeRaceService.getCurrentRefereeRaces());
    }

    @GetMapping("/races/{raceId}/entries")
    @PreAuthorize("hasRole('REFEREE')")
    public ResponseEntity<List<RaceEntryResponse>> getAssignedRaceEntries(@PathVariable Long raceId) {
        return ResponseEntity.ok(refereeRaceService.getEntriesForAssignedRace(raceId));
    }
}
