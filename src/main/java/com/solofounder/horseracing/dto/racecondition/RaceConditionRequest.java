package com.solofounder.horseracing.dto.racecondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceConditionRequest {

    @NotBlank(message = "Race condition name is required")
    private String conditionName;

    @NotNull(message = "Distance is required")
    private Integer distance;

    private String trackType;
    private Short minEntries;
    private Short maxEntries;
    private String classRequirement;
}
