package com.solofounder.horseracing.dto.entry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRaceEntryRequest {

    private Long registrationId;

    @NotNull(message = "Invitation ID is required")
    private Long invitationId;

    @Min(value = 1, message = "Gate number must be at least 1")
    private Short gateNumber;

    @DecimalMin(value = "30.0", message = "Handicap weight must be at least 30.0")
    private BigDecimal handicapWeight;
}
