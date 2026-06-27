package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.racecondition.RaceConditionRequest;
import com.solofounder.horseracing.dto.racecondition.RaceConditionResponse;
import com.solofounder.horseracing.service.RaceConditionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/race-conditions")
@RequiredArgsConstructor
public class AdminRaceConditionController {

    private final RaceConditionService raceConditionService;

    @GetMapping
    public ResponseEntity<List<RaceConditionResponse>> getAllRaceConditions() {
        return ResponseEntity.ok(raceConditionService.getAllRaceConditions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RaceConditionResponse> getRaceCondition(@PathVariable Long id) {
        return ResponseEntity.ok(raceConditionService.getRaceCondition(id));
    }

    @PostMapping
    public ResponseEntity<RaceConditionResponse> createRaceCondition(@Valid @RequestBody RaceConditionRequest request) {
        return ResponseEntity.ok(raceConditionService.createRaceCondition(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RaceConditionResponse> updateRaceCondition(@PathVariable Long id,
                                                                     @Valid @RequestBody RaceConditionRequest request) {
        return ResponseEntity.ok(raceConditionService.updateRaceCondition(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRaceCondition(@PathVariable Long id) {
        raceConditionService.deleteRaceCondition(id);
        return ResponseEntity.noContent().build();
    }
}
