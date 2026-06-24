package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.service.RaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
public class RaceController {

    private final RaceService raceService;

    /**
     * GET /api/races — trả về tất cả races, dành cho jockey/owner/staff/admin xem thông tin.
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RaceResponse>> getAllRaces() {
        return ResponseEntity.ok(raceService.getAllRaces());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RaceResponse> getRace(@PathVariable Long id) {
        return ResponseEntity.ok(raceService.getRace(id));
    }

    /**
     * GET /api/races/open — chỉ trả về races đang mở đăng ký theo thời gian mở/đóng.
     */
    @GetMapping("/open")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RaceResponse>> getOpenRaces() {
        return ResponseEntity.ok(raceService.getOpenRaces());
    }
}
