package com.solofounder.horseracing.dto.entry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefereePreCheckRequest {

    @DecimalMin(value = "0.01", message = "Handicap weight must be greater than 0")
    private BigDecimal handicapWeight;

    @NotNull(message = "Actual weight is required")
    @DecimalMin(value = "0.01", message = "Actual weight must be greater than 0")
    private BigDecimal actualWeight;

    private BigDecimal leadWeight;

    private BigDecimal carriedWeight;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}
