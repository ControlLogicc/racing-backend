package com.solofounder.horseracing.dto.jockey;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJockeyRequest {
    private BigDecimal weight;
    private Short experienceYears;
    private String status;
}
