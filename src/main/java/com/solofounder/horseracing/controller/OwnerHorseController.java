package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.horse.CreateHorseRequest;
import com.solofounder.horseracing.dto.horse.UpdateHorseRequest;
import com.solofounder.horseracing.dto.horse.HorseResponse;
import com.solofounder.horseracing.service.HorseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/horses")
@RequiredArgsConstructor
public class OwnerHorseController {

    private final HorseService horseService;

    @GetMapping
    public ResponseEntity<List<HorseResponse>> getOwnerHorses() {
        return ResponseEntity.ok(horseService.getOwnerHorses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorseResponse> getOwnerHorse(@PathVariable Long id) {
        return ResponseEntity.ok(horseService.getOwnerHorse(id));
    }

    @PostMapping
    public ResponseEntity<HorseResponse> createOwnerHorse(@Valid @RequestBody CreateHorseRequest request) {
        return ResponseEntity.ok(horseService.createOwnerHorse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorseResponse> updateOwnerHorse(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateHorseRequest request) {
        return ResponseEntity.ok(horseService.updateOwnerHorse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwnerHorse(@PathVariable Long id) {
        horseService.deleteOwnerHorse(id);
        return ResponseEntity.noContent().build();
    }
}
