package com.solofounder.horseracing.dto.registration;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRegistrationRequest {

    @NotNull(message = "Race id is required")
    private Long raceId;

    @NotNull(message = "Horse id is required")
    private Long horseId;
}
