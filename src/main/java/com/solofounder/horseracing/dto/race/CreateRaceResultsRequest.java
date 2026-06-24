package com.solofounder.horseracing.dto.race;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRaceResultsRequest {

    @Valid
    @NotEmpty(message = "Results are required")
    private List<CreateRaceResultRequest> results;
}
