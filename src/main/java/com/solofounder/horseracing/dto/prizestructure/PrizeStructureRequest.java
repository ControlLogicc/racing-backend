package com.solofounder.horseracing.dto.prizestructure;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrizeStructureRequest {

    @NotNull(message = "Race id is required")
    private Long raceId;

    @NotNull(message = "Position is required")
    private Short position;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Score is required")
    private BigDecimal score;
}
