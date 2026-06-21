package com.solofounder.horseracing.dto.staff;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRegistrationResponse {
    private Long registrationId;
    private Long raceId;
    private String raceName;
    private Long horseId;
    private String horseName;
    private Long ownerId;
    private String ownerName;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private Boolean canApprove;
    private Boolean canReject;
}
