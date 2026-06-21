package com.solofounder.horseracing.dto.invitation;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    private Long invitationId;
    private Long raceRegistrationId;
    private Long raceId;
    private String raceName;
    private Long horseId;
    private String horseName;
    private Long ownerId;
    private String ownerName;
    private Long jockeyId;
    private String jockeyName;
    private String status;
    private String message;
    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private Boolean canAccept;
    private Boolean canDecline;
}
