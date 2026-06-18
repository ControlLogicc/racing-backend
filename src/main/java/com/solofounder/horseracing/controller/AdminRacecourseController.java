package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.racecourse.RacecourseRequest;
import com.solofounder.horseracing.dto.racecourse.RacecourseResponse;
import com.solofounder.horseracing.service.RacecourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/racecourses")
@RequiredArgsConstructor
public class AdminRacecourseController {

    private final RacecourseService racecourseService;

    @GetMapping
    public ResponseEntity<List<RacecourseResponse>> getAllRacecourses() {
        return ResponseEntity.ok(racecourseService.getAllRacecourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RacecourseResponse> getRacecourse(@PathVariable Long id) {
        return ResponseEntity.ok(racecourseService.getRacecourse(id));
    }

    @PostMapping
    public ResponseEntity<RacecourseResponse> createRacecourse(@Valid @RequestBody RacecourseRequest request) {
        return ResponseEntity.ok(racecourseService.createRacecourse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RacecourseResponse> updateRacecourse(@PathVariable Long id,
                                                               @Valid @RequestBody RacecourseRequest request) {
        return ResponseEntity.ok(racecourseService.updateRacecourse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRacecourse(@PathVariable Long id) {
        racecourseService.deleteRacecourse(id);
        return ResponseEntity.noContent().build();
    }
}
