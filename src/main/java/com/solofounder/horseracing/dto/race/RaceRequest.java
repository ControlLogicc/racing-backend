package com.solofounder.horseracing.dto.race;

import jakarta.validation.constraints.Min;
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

    @NotNull(message = "Race number is required")
    @Min(value = 1, message = "Race number must be positive")
    private Short raceNo;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;

    @NotNull(message = "Registration open date-time is required")
    private LocalDateTime registrationOpenAt;

    @NotNull(message = "Registration close date-time is required")
    private LocalDateTime registrationCloseAt;

    private String status;
}
