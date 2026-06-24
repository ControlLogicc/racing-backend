package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.dto.staff.CreateStaffRequest;
import com.solofounder.horseracing.dto.staff.StaffResponse;
import com.solofounder.horseracing.dto.staff.StaffRegistrationResponse;
import com.solofounder.horseracing.dto.staff.UpdateStaffRequest;
import com.solofounder.horseracing.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    // ─── Admin: lấy toàn bộ danh sách staff ──────────────────────────────────
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffResponse>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    // ─── Staff: lấy profile của chính mình ───────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> getProfileOrAllStaff() {
        return ResponseEntity.ok(staffService.getCurrentProfileOrAllStaff());
    }

    @GetMapping("/races")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<RaceResponse>> getMyRaces() {
        return ResponseEntity.ok(staffService.getCurrentStaffRaces());
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<StaffRegistrationResponse>> getMyRegistrations() {
        return ResponseEntity.ok(staffService.getCurrentStaffRegistrations());
    }

    @PostMapping
    public ResponseEntity<StaffResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(staffService.createStaff(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponse> updateStaff(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }
}
