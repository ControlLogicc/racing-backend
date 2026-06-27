package com.solofounder.horseracing.dto.entry;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JockeyWeightCheckResponse {
    private BigDecimal handicapWeight;
    private BigDecimal jockeyActualWeight;
    private BigDecimal leadWeight;
    private BigDecimal carriedWeight;
    private String weightCheckStatus;
}
