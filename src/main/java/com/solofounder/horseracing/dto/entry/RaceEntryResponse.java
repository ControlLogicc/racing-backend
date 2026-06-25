package com.solofounder.horseracing.dto.entry;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceEntryResponse {
    private Long entryId;
    private Long raceId;
    private String raceName;
    private Long registrationId;
    private Long invitationId;
    private Long horseId;
    private String horseName;
    private Long jockeyId;
    private String jockeyName;
    private Short gateNumber;
    private BigDecimal handicapWeight;
    private BigDecimal jockeyActualWeight;
    private BigDecimal leadWeight;
    private BigDecimal carriedWeight;
    private String weightCheckStatus;
    private Long weightCheckedBy;
    private String weightCheckedByName;
    private LocalDateTime weightCheckedAt;
    private String entryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
