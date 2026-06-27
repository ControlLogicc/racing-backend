package com.solofounder.horseracing.dto.registration;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    private Long registrationId;
    private Long raceId;
    private String raceName;
    private Long horseId;
    private String horseName;
    private Long ownerId;
    private String ownerName;
    private String registrationStatus;
    // Keep "status" as alias for frontend compatibility
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime scheduledTime;
    private LocalDateTime registrationCloseAt;
    // Invitation info (populated from latest invitation)
    private Long invitationId;
    private String invitationStatus;
    // Entry info
    private Long entryId;
    // Race status
    private String raceStatus;
    // Derived flag: can owner invite a jockey?
    private boolean canInviteJockey;
}
