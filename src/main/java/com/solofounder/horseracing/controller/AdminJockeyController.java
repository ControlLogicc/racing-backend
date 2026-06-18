package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.jockey.CreateJockeyRequest;
import com.solofounder.horseracing.dto.jockey.JockeyResponse;
import com.solofounder.horseracing.dto.jockey.UpdateJockeyRequest;
import com.solofounder.horseracing.service.JockeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/jockeys")
@RequiredArgsConstructor
public class AdminJockeyController {

    private final JockeyService jockeyService;

    @GetMapping
    public ResponseEntity<List<JockeyResponse>> getAllJockeys() {
        return ResponseEntity.ok(jockeyService.getAllJockeys());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JockeyResponse> getJockey(@PathVariable Long id) {
        return ResponseEntity.ok(jockeyService.getJockey(id));
    }

    @PostMapping
    public ResponseEntity<JockeyResponse> createJockey(@Valid @RequestBody CreateJockeyRequest request) {
        return ResponseEntity.ok(jockeyService.createJockey(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JockeyResponse> updateJockey(@PathVariable Long id,
                                                       @RequestBody UpdateJockeyRequest request) {
        return ResponseEntity.ok(jockeyService.updateJockey(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJockey(@PathVariable Long id) {
        jockeyService.deleteJockey(id);
        return ResponseEntity.noContent().build();
    }
}
