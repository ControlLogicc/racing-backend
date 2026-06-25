package com.solofounder.horseracing.dto.jockey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleJockeyForInvitationResponse {
    private Long jockeyRaceRegistrationId;
    private Long registrationId;
    private Long raceId;
    private Long jockeyId;
    private String jockeyName;
    private BigDecimal weight;
    private Integer experienceYears;
    private String jockeyStatus;
    private String raceRegistrationStatus;
    private LocalDateTime registeredAt;
    private Boolean canInvite;
    private String reason;
}
