package com.solofounder.horseracing.dto.registration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedRegistrationForInvitationResponse {
    private Long registrationId;
    private Long raceId;
    private String raceName;
    private Long horseId;
    private String horseName;
    private String registrationStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime registrationCloseAt;
    private LocalDateTime scheduledTime;
    private Long invitationId;
    private String invitationStatus;
    private Long entryId;
    private Boolean canInviteJockey;
}
