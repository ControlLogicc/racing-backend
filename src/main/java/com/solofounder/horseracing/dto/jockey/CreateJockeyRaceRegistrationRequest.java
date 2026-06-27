package com.solofounder.horseracing.dto.jockey;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJockeyRaceRegistrationRequest {

    @NotNull(message = "Race id is required")
    private Long raceId;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
