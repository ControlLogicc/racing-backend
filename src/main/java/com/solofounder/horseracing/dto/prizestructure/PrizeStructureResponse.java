package com.solofounder.horseracing.dto.prizestructure;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrizeStructureResponse {
    private Long prizeId;
    private Long raceId;
    private String raceName;
    private Short position;
    private BigDecimal amount;
    private BigDecimal score;
    private LocalDateTime createdAt;
}
