package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.entry.*;
import com.solofounder.horseracing.service.RaceEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class RaceEntryController {

    private final RaceEntryService raceEntryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RaceEntryResponse> createEntry(@Valid @RequestBody CreateRaceEntryRequest request) {
        return ResponseEntity.ok(raceEntryService.createEntry(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'OWNER', 'JOCKEY')")
    public ResponseEntity<RaceEntryResponse> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(raceEntryService.getEntry(id));
    }

    @GetMapping("/race/{raceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'OWNER', 'JOCKEY')")
    public ResponseEntity<List<RaceEntryResponse>> getEntriesForRace(@PathVariable Long raceId) {
        return ResponseEntity.ok(raceEntryService.getEntriesForRace(raceId));
    }

    @GetMapping("/race/{raceId}/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<AcceptedInvitationCandidateResponse>> getAcceptedInvitationCandidates(
            @PathVariable Long raceId) {
        return ResponseEntity.ok(raceEntryService.getAcceptedInvitationCandidates(raceId));
    }

    @PutMapping("/race/{raceId}/weight-check")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'REFEREE')")
    public ResponseEntity<List<RaceEntryResponse>> batchWeightCheck(@PathVariable Long raceId,
                                                                    @Valid @RequestBody BatchWeightCheckRequest request) {
        return ResponseEntity.ok(raceEntryService.batchWeightCheck(raceId, request));
    }

    @PutMapping("/{id}/weight")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RaceEntryResponse> updateWeight(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateWeightRequest request) {
        return ResponseEntity.ok(raceEntryService.updateWeight(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RaceEntryResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(raceEntryService.updateStatus(id, request));
    }
}
