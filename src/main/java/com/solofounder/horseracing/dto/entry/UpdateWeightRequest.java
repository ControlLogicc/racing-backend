package com.solofounder.horseracing.dto.entry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWeightRequest {

    @NotNull(message = "Actual weight is required")
    @DecimalMin(value = "30.0", message = "Actual weight must be at least 30.0")
    private BigDecimal actualWeight;

    @NotBlank(message = "Weight check status is required")
    @Pattern(regexp = "^(passed|failed|overweight_accepted)$", message = "Weight check status must be passed, failed, or overweight_accepted")
    private String weightCheckStatus;
}
