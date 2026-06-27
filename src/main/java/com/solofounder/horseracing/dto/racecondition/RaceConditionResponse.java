package com.solofounder.horseracing.dto.racecondition;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceConditionResponse {
    private Long conditionId;
    private String conditionName;
    private Integer distance;
    private String trackType;
    private Short minEntries;
    private Short maxEntries;
    private String classRequirement;
    private LocalDateTime createdAt;
}
