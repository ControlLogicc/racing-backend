package com.solofounder.horseracing.controller;

import com.solofounder.horseracing.dto.admin.CreateInternalAccountRequest;
import com.solofounder.horseracing.dto.admin.InternalAccountResponse;
import com.solofounder.horseracing.service.InternalAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/internal-accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final InternalAccountService internalAccountService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InternalAccountResponse> createInternalAccount(
            @Valid @RequestBody CreateInternalAccountRequest request) {
        return ResponseEntity.ok(internalAccountService.createInternalAccount(request));
    }
}
