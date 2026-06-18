package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.season.SeasonRequest;
import com.solofounder.horseracing.dto.season.SeasonResponse;
import com.solofounder.horseracing.service.SeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seasons")
@RequiredArgsConstructor
public class AdminSeasonController {

    private final SeasonService seasonService;

    @GetMapping
    public ResponseEntity<List<SeasonResponse>> getAllSeasons() {
        return ResponseEntity.ok(seasonService.getAllSeasons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeasonResponse> getSeason(@PathVariable Long id) {
        return ResponseEntity.ok(seasonService.getSeason(id));
    }

    @PostMapping
    public ResponseEntity<SeasonResponse> createSeason(@Valid @RequestBody SeasonRequest request) {
        return ResponseEntity.ok(seasonService.createSeason(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeasonResponse> updateSeason(@PathVariable Long id,
                                                       @Valid @RequestBody SeasonRequest request) {
        return ResponseEntity.ok(seasonService.updateSeason(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeason(@PathVariable Long id) {
        seasonService.deleteSeason(id);
        return ResponseEntity.noContent().build();
    }
}
