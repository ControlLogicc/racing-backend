package com.solofounder.horseracing.dto.entry;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JockeyWeightCheckResponse {
    private Long entryId;
    private BigDecimal handicapWeight;
    private BigDecimal actualWeight;
    private BigDecimal jockeyActualWeight;
    private BigDecimal leadWeight;
    private BigDecimal carriedWeight;
    private BigDecimal overweightAmount;
    private String weightCheckStatus;
    private String entryStatus;
    private String horseName;
    private String jockeyName;
    private String note;
}
