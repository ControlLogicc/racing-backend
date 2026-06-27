package com.solofounder.horseracing.dto.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptedInvitationCandidateResponse {

    private Long invitationId;
    private Long registrationId;
    private Long raceId;
    private String raceName;
    private Long horseId;
    private String horseName;
    private Long ownerId;
    private String ownerName;
    private Long jockeyId;
    private String jockeyName;
    private String invitationStatus;
    private String registrationStatus;
    private Boolean canCreateEntry;
    private String reason;
}
