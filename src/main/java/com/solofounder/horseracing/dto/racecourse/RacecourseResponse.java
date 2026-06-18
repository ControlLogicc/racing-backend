package com.solofounder.horseracing.dto.racecourse;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RacecourseResponse {
    private Long racecourseId;
    private String racecourseName;
    private String location;
    private String surfaceType;
    private Integer capacity;
    private LocalDateTime createdAt;
}
