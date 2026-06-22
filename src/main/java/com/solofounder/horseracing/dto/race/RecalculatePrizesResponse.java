package com.solofounder.horseracing.dto.race;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalculatePrizesResponse {
    private Long raceId;
    private String raceName;
    private Integer processedResults;
    private Integer updatedHorseCount;
    private BigDecimal totalPrizeAmount;
    private BigDecimal totalScoreAwarded;
    private String message;
}
