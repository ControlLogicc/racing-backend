package com.solofounder.horseracing.dto.entry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JockeyWeightCheckRequest {

    @NotNull(message = "Jockey actual weight is required")
    @DecimalMin(value = "0.01", message = "Jockey actual weight must be greater than 0")
    private BigDecimal jockeyActualWeight;
}
