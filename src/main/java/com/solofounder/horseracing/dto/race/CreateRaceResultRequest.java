package com.solofounder.horseracing.dto.race;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRaceResultRequest {

    @NotNull(message = "Entry id is required")
    private Long entryId;

    @NotNull(message = "Position is required")
    @Min(value = 1, message = "Position must be at least 1")
    private Short position;

    private LocalTime finishTime;

    private String resultStatus;
}
