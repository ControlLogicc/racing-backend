package com.solofounder.horseracing.dto.horse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyHorseRatingRequest {

    /**
     * Optional: Staff can override the Owner's claimedScore.
     * If null, the Owner's claimedScore is used as-is.
     */
    @DecimalMin(value = "0.0", message = "Approved score must be >= 0")
    @Schema(example = "35.0", description = "Override score (optional). Uses Owner's claimed score if omitted.")
    private BigDecimal approvedScore;

    /**
     * Optional: Staff can override the Owner's claimedClass.
     * If null, the Owner's claimedClass is used as-is.
     */
    @Min(value = 1, message = "Approved class must be between 1 and 5")
    @Max(value = 5, message = "Approved class must be between 1 and 5")
    @Schema(example = "4", minimum = "1", maximum = "5",
            description = "Override class (optional). Uses Owner's claimed class if omitted.")
    private Short approvedClass;
}
