package com.solofounder.horseracing.dto.jockey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JockeyRaceRegistrationResponse {
    private Long jockeyRaceRegistrationId;
    private Long raceId;
    private String raceName;
    private Long jockeyId;
    private String jockeyName;
    private String jockeyStatus;
    private String registrationStatus;
    private String note;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
}
