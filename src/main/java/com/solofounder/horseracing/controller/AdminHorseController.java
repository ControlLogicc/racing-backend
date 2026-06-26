package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.horse.UpdateHorseRequest;
import com.solofounder.horseracing.dto.horse.HorseResponse;
import com.solofounder.horseracing.service.HorseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/horses")
@RequiredArgsConstructor
public class AdminHorseController {

    private final HorseService horseService;

    @GetMapping
    public ResponseEntity<List<HorseResponse>> getAllHorses() {
        return ResponseEntity.ok(horseService.getAllHorses());
    }

    @GetMapping("/pending-rating")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HorseResponse>> getPendingRatingHorses() {
        return ResponseEntity.ok(horseService.getPendingRatingHorses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorseResponse> getHorse(@PathVariable Long id) {
        return ResponseEntity.ok(horseService.getHorse(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorseResponse> updateHorse(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateHorseRequest request) {
        return ResponseEntity.ok(horseService.updateHorse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHorse(@PathVariable Long id) {
        horseService.deleteHorseByAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
