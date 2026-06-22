package com.solofounder.horseracing.dto.entry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightCheckItemRequest {

    @NotNull(message = "Entry ID is required")
    private Long entryId;

    @NotNull(message = "Actual weight is required")
    @DecimalMin(value = "0.01", message = "Actual weight must be greater than 0")
    private BigDecimal actualWeight;

    @NotNull(message = "Passed value is required")
    private Boolean passed;

    private String note;
}
