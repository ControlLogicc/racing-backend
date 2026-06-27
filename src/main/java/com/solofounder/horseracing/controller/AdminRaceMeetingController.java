package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.racemeeting.RaceMeetingRequest;
import com.solofounder.horseracing.dto.racemeeting.RaceMeetingResponse;
import com.solofounder.horseracing.service.RaceMeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/race-meetings")
@RequiredArgsConstructor
public class AdminRaceMeetingController {

    private final RaceMeetingService raceMeetingService;

    @GetMapping
    public ResponseEntity<List<RaceMeetingResponse>> getAllRaceMeetings() {
        return ResponseEntity.ok(raceMeetingService.getAllRaceMeetings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RaceMeetingResponse> getRaceMeeting(@PathVariable Long id) {
        return ResponseEntity.ok(raceMeetingService.getRaceMeeting(id));
    }

    @PostMapping
    public ResponseEntity<RaceMeetingResponse> createRaceMeeting(@Valid @RequestBody RaceMeetingRequest request) {
        return ResponseEntity.ok(raceMeetingService.createRaceMeeting(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RaceMeetingResponse> updateRaceMeeting(@PathVariable Long id,
                                                                 @Valid @RequestBody RaceMeetingRequest request) {
        return ResponseEntity.ok(raceMeetingService.updateRaceMeeting(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRaceMeeting(@PathVariable Long id) {
        raceMeetingService.deleteRaceMeeting(id);
        return ResponseEntity.noContent().build();
    }
}
