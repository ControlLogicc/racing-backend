package com.solofounder.horseracing.dto.racecourse;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RacecourseRequest {

    @NotBlank(message = "Racecourse name is required")
    private String racecourseName;

    private String location;
    private String surfaceType;
    private Integer capacity;
}
