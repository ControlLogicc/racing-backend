package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.dto.referee.CreateRefereeRequest;
import com.solofounder.horseracing.dto.referee.RefereeResponse;
import com.solofounder.horseracing.dto.referee.UpdateRefereeRequest;
import com.solofounder.horseracing.service.RefereeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referees")
@RequiredArgsConstructor
public class RefereeController {

    private final RefereeService refereeService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('REFEREE')")
    public ResponseEntity<RefereeResponse> getCurrentReferee() {
        return ResponseEntity.ok(refereeService.getCurrentReferee());
    }

    @GetMapping("/races")
    @PreAuthorize("hasRole('REFEREE')")
    public ResponseEntity<List<RaceResponse>> getCurrentRefereeRaces() {
        return ResponseEntity.ok(refereeService.getCurrentRefereeRaces());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<RefereeResponse>> getAllReferees() {
        return ResponseEntity.ok(refereeService.getAllReferees());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefereeResponse> createReferee(@Valid @RequestBody CreateRefereeRequest request) {
        return ResponseEntity.ok(refereeService.createReferee(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefereeResponse> updateReferee(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateRefereeRequest request) {
        return ResponseEntity.ok(refereeService.updateReferee(id, request));
    }
}
