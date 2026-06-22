package com.solofounder.horseracing.dto.entry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchWeightCheckRequest {

    @NotNull(message = "Handicap weight is required")
    @DecimalMin(value = "0.01", message = "Handicap weight must be greater than 0")
    private BigDecimal handicapWeight;

    @NotEmpty(message = "Weight checks are required")
    @Valid
    private List<WeightCheckItemRequest> checks;
}
