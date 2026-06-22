package com.solofounder.horseracing.dto.race;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceResultResponse {

    private Long resultId;
    private Long entryId;
    private Long raceId;
    private String raceName;
    private Long horseId;
    private String horseName;
    private Long jockeyId;
    private String jockeyName;
    private Short position;
    private LocalTime finishTime;
    private String resultStatus;
    private BigDecimal prizeAmount;
    private BigDecimal scoreAwarded;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
