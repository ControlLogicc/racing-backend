package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.invitation.CreateInvitationRequest;
import com.solofounder.horseracing.dto.invitation.InvitationResponse;
import com.solofounder.horseracing.service.RaceInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class RaceInvitationController {

    private final RaceInvitationService raceInvitationService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InvitationResponse> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request) {
        return ResponseEntity.ok(raceInvitationService.createInvitation(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('JOCKEY') or hasRole('OWNER')")
    public ResponseEntity<List<InvitationResponse>> getInvitations(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(raceInvitationService.getInvitations(status));
    }

    @PutMapping("/{invitationId}/accept")
    @PreAuthorize("hasRole('JOCKEY')")
    public ResponseEntity<InvitationResponse> acceptInvitation(
            @PathVariable Long invitationId) {
        return ResponseEntity.ok(raceInvitationService.acceptInvitation(invitationId));
    }

    @PutMapping("/{invitationId}/decline")
    @PreAuthorize("hasRole('JOCKEY')")
    public ResponseEntity<InvitationResponse> declineInvitation(
            @PathVariable Long invitationId) {
        return ResponseEntity.ok(raceInvitationService.declineInvitation(invitationId));
    }
}
