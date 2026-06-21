package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.report.CreateRefereeReportRequest;
import com.solofounder.horseracing.dto.report.RefereeReportResponse;
import com.solofounder.horseracing.dto.report.UpdateRefereeReportRequest;
import com.solofounder.horseracing.service.RefereeReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class RefereeReportController {

    private final RefereeReportService refereeReportService;

    @PostMapping
    @PreAuthorize("hasRole('REFEREE')")
    public ResponseEntity<RefereeReportResponse> createReport(
            @Valid @RequestBody CreateRefereeReportRequest request) {
        return ResponseEntity.ok(refereeReportService.createReport(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RefereeReportResponse>> getAllReports() {
        return ResponseEntity.ok(refereeReportService.getAllReports());
    }

    @GetMapping("/{raceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REFEREE', 'STAFF')")
    public ResponseEntity<List<RefereeReportResponse>> getReportsByRace(@PathVariable Long raceId) {
        return ResponseEntity.ok(refereeReportService.getReportsByRace(raceId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REFEREE')")
    public ResponseEntity<RefereeReportResponse> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRefereeReportRequest request) {
        return ResponseEntity.ok(refereeReportService.updateReport(id, request));
    }
}
