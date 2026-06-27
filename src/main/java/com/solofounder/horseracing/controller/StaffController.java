package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.horse.HorseResponse;
import com.solofounder.horseracing.dto.horse.VerifyHorseRatingRequest;
import com.solofounder.horseracing.dto.race.RaceResponse;
import com.solofounder.horseracing.dto.staff.CreateStaffRequest;
import com.solofounder.horseracing.dto.staff.StaffResponse;
import com.solofounder.horseracing.dto.staff.StaffRegistrationResponse;
import com.solofounder.horseracing.dto.staff.UpdateStaffRequest;
import com.solofounder.horseracing.service.HorseService;
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
    private final HorseService horseService;

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

    /**
     * List all PREVIOUSLY_REGISTERED horses waiting for Staff rating review.
     * GET /api/staff/horses/pending
     *
     * Response fields of interest:
     *   - horseId, horseName, ownerName
     *   - registrationType  → always "PREVIOUSLY_REGISTERED"
     *   - claimedScore      → Rating the Owner submitted (awaiting review)
     *   - claimedClass      → Class the Owner submitted
     *   - currentScore      → Current official score (still 0 until approved)
     *   - status            → "FAIL" while pending, "ACTIVE" once approved
     *   - ratingVerified    → false while pending
     */
    @GetMapping("/horses/pending")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<HorseResponse>> getPendingHorses() {
        return ResponseEntity.ok(horseService.getPendingHorses());
    }

    /**
     * Verify (approve) rating of a PREVIOUSLY_REGISTERED horse.
     * Staff can optionally override the Owner's claimed score/class.
     * PUT /api/staff/horses/{horseId}/verify-rating
     */
    @PutMapping("/horses/{horseId}/verify-rating")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<HorseResponse> verifyHorseRating(
            @PathVariable Long horseId,
            @Valid @RequestBody(required = false) VerifyHorseRatingRequest request) {
        return ResponseEntity.ok(horseService.verifyHorseRating(horseId, request));
    }

    /**
     * Reject the claimed rating of a PREVIOUSLY_REGISTERED horse.
     * The horse is NOT deleted; Owner can resubmit corrected claim.
     * PUT /api/staff/horses/{horseId}/reject-rating
     */
    @PutMapping("/horses/{horseId}/reject-rating")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<HorseResponse> rejectHorseRating(@PathVariable Long horseId) {
        return ResponseEntity.ok(horseService.rejectHorseRating(horseId));
    }
}
