package com.solofounder.horseracing.dto.race;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceRequest {

    @NotNull(message = "Race meeting id is required")
    private Long meetingId;

    @NotNull(message = "Race condition id is required")
    private Long conditionId;

    private Long staffId;
    private Long refereeId;

    @NotBlank(message = "Race name is required")
    private String raceName;

    private Short raceNo;
    private LocalDateTime scheduledTime;
    private String status;
}
