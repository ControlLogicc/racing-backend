package com.solofounder.horseracing.dto.jockey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JockeyStatsResponse {
    private Long jockeyId;
    private String jockeyName;
    private Long totalRaces;
    private Long totalWins;
    private Long top3Finishes;
    private Long disqualifiedCount;
    private BigDecimal totalPrizeAmount;
    private BigDecimal totalScoreAwarded;
    private BigDecimal averagePosition;
    private BigDecimal winRate;
}
