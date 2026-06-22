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
    private Short drawNumber;
    private BigDecimal handicapWeight;
    private BigDecimal actualWeight;
    private String weightCheckStatus;
    private String entryStatus;
    private Long confirmedByStaffId;
    private String confirmedByStaffName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
