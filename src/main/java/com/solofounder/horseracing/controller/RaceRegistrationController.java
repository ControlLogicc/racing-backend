package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.registration.CreateRegistrationRequest;
import com.solofounder.horseracing.dto.registration.RegistrationResponse;
import com.solofounder.horseracing.service.RaceRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RaceRegistrationController {

    private final RaceRegistrationService raceRegistrationService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RegistrationResponse> createRegistration(@Valid @RequestBody CreateRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(raceRegistrationService.createRegistration(request));
    }

    @GetMapping("/{raceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<RegistrationResponse>> getRegistrationsForRace(@PathVariable Long raceId) {
        return ResponseEntity.ok(raceRegistrationService.getRegistrationsForRace(raceId));
    }

    @PutMapping("/{registrationId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RegistrationResponse> approveRegistration(@PathVariable Long registrationId) {
        return ResponseEntity.ok(raceRegistrationService.approveRegistration(registrationId));
    }

    @PutMapping("/{registrationId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RegistrationResponse> rejectRegistration(@PathVariable Long registrationId) {
        return ResponseEntity.ok(raceRegistrationService.rejectRegistration(registrationId));
    }
}
