package com.solofounder.horseracing.dto.racemeeting;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceMeetingResponse {
    private Long meetingId;
    private Long seasonId;
    private String seasonName;
    private Long racecourseId;
    private String racecourseName;
    private LocalDate meetingDate;
    private String status;
    private LocalDateTime createdAt;
}
