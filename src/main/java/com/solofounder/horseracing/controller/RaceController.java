package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.service.RaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
public class RaceController {

    private final RaceService raceService;

    @GetMapping("/open")
    public ResponseEntity<List<RaceResponse>> getOpenRaces() {
        return ResponseEntity.ok(raceService.getOpenRaces());
    }
}
