package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.registration.ApprovedRegistrationForInvitationResponse;
import com.solofounder.horseracing.service.RaceRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner/registrations")
@RequiredArgsConstructor
public class OwnerRegistrationController {

    private final RaceRegistrationService raceRegistrationService;

    @GetMapping("/approved")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<ApprovedRegistrationForInvitationResponse>> getApprovedRegistrationsForInvitation() {
        return ResponseEntity.ok(raceRegistrationService.getApprovedRegistrationsForCurrentOwner());
    }
}
