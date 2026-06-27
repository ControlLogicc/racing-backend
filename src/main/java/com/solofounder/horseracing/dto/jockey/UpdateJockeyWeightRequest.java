package com.solofounder.horseracing.dto.jockey;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJockeyWeightRequest {

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "30.0", message = "Weight must be at least 30")
    @DecimalMax(value = "80.0", message = "Weight must be at most 80")
    private BigDecimal weight;
}
