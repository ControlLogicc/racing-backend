package com.solofounder.horseracing.dto.racemeeting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceMeetingRequest {

    @NotBlank(message = "Meeting name is required")
    private String meetingName;

    @NotNull(message = "Season id is required")
    private Long seasonId;

    @NotNull(message = "Racecourse id is required")
    private Long racecourseId;

    @NotNull(message = "Meeting date is required")
    private LocalDate meetingDate;
}
